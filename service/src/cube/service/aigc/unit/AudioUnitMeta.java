/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.unit;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.common.Language;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.aigc.listener.VoiceDiarizationListener;
import cube.service.aigc.utils.VoiceDiarizationIndicator;
import cube.util.FileUtils;
import cube.util.TextUtils;
import cube.util.TimeUtils;
import org.json.JSONObject;

import java.util.*;

/**
 * 音频单元任务元数据。
 *
 * <p><b>平台职责边界</b>：说话人分离是宿主的平台级基础能力，本类只做
 * 平台侧收尾——结果修补（过滤日语词、剔除纯标点段）、说话人标签归一化、
 * 可选的指标分析、显示名优化、模块级监听器扇出、请求级回调与落库。
 * <b>不含任何业务角色分类</b>（如「来访者 / 咨询师」映射），该类收尾操作
 * 由业务模块经 {@code SpeechModuleListener} 自行订阅完成。</p>
 */
public class AudioUnitMeta extends UnitMeta {

    public final long timestamp;

    protected AuthToken authToken;

    protected FileLabel file;

    protected boolean preprocess;

    protected boolean storage;

    /**
     * 是否分析内容语料的正负面 / 中性指标。
     *
     * <p>指标分析对每段内容调用一次文本生成，成本较高，故为可选功能；
     * 关闭时结果中的 {@code indicator} 保持 {@code null}。</p>
     */
    protected boolean sentiment;

    protected AIGCAction action;

    public VoiceDiarizationListener voiceDiarizationListener;

    public AudioUnitMeta(AIGCService service, AIGCUnit unit, AuthToken authToken, AIGCAction action,
                         FileLabel file, boolean preprocess, boolean storage, boolean sentiment) {
        super(service, unit);
        this.timestamp = System.currentTimeMillis();
        this.authToken = authToken;
        this.action = action;
        this.file = file;
        this.preprocess = preprocess;
        this.storage = storage;
        this.sentiment = sentiment;
    }

    public FileLabel getFile() {
        return this.file;
    }

    @Override
    public void process() {
        if (null == this.voiceDiarizationListener) {
            Logger.w(this.getClass(), "#process - The listener is null: " + this.file.getFileCode());
        }

        // 计算等待时长，74秒的MP3文件，大约 302192 字节，耗时约2分10秒（130秒）
        final double factor = (130.0 * 1000.0) / 302192;
        double offset = ((double) this.file.getFileSize()) / factor;
        long timeout = (5 * 60 * 1000) + ((long) offset);

        JSONObject data = new JSONObject();
        data.put("fileLabel", this.file.toJSON());
        data.put("preprocess", this.preprocess);
        Packet request = new Packet(this.action.name, data);
        ActionDialect dialect = this.service.getCellet().transmit(this.unit.getContext(), request.toDialect(), timeout);
        if (null == dialect) {
            Logger.w(AIGCService.class, "#process - Audio unit error: " + this.file.getFileCode());
            // 回调错误
            if (null != this.voiceDiarizationListener) {
                this.voiceDiarizationListener.onFailed(this.file, AIGCStateCode.UnitError);
            }
            return;
        }

        Packet response = new Packet(dialect);
        if (AIGCStateCode.Ok.code != Packet.extractCode(response)) {
            Logger.w(AIGCService.class, "#process - Audio unit failed: " + this.file.getFileCode());
            // 回调错误
            if (null != this.voiceDiarizationListener) {
                this.voiceDiarizationListener.onFailed(this.file, AIGCStateCode.Failure);
            }
            return;
        }

        JSONObject payload = Packet.extractDataPayload(response);

        if (this.action == AIGCAction.SpeechDiarization) {
            final VoiceDiarization result = new VoiceDiarization(payload.getJSONObject("result"));
            // 补齐参数
            result.contactId = this.authToken.getContactId();
            result.setDomain(this.authToken.getDomain());
            String nameCode = FileUtils.extractFileName(this.file.getFileName());
            if (nameCode.length() > 10) {
                nameCode = nameCode.substring(0, 9);
            }
            result.title = "Voice-" + nameCode + "-" + TimeUtils.formatDateForPathSymbol(result.getTimestamp());
            result.remark = "";

            if (Logger.isDebugLevel()) {
                Logger.d(this.getClass(), "#process - Speaker Diarization result\nfile: " + result.file.getFileCode() +
                        "\nelapsed: " + result.elapsed +
                        "\ntracks: " + result.tracks.size() +
                        "\nduration: " + result.duration);
            }

            // 排除日语
            Iterator<VoiceTrack> iter = result.tracks.iterator();
            while (iter.hasNext()) {
                VoiceTrack track = iter.next();
                List<String> words = track.recognition.words;
                Iterator<String> wordIter = words.iterator();
                while (wordIter.hasNext()) {
                    String word = wordIter.next();
                    if (TextUtils.isJapanese(word)) {
                        track.recognition.text = track.recognition.text.replace(word, "");
                        wordIter.remove();
                    }
                }

                if (track.recognition.text.length() == 1 &&
                        (TextUtils.containsChinesePunctuation(track.recognition.text)) ||
                        (TextUtils.containsEnglishPunctuation(track.recognition.text))) {
                    // 只有一个字符，且是标点符号，则删除
                    iter.remove();
                }
            }

            if (result.tracks.isEmpty()) {
                this.service.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        Logger.d(this.getClass(), "#process - Diarization tracks length is 0: " + file.getFileCode());

                        // 设置指标
                        if (AudioUnitMeta.this.sentiment) {
                            result.indicator = new VoiceDiarizationIndicator(result.getId());
                        }

                        // 模块级监听器扇出：先于请求级回调，模块可在此做收尾
                        AudioUnitMeta.this.service.notifyDiarizationListeners(file, result);

                        if (null != voiceDiarizationListener) {
                            voiceDiarizationListener.onCompleted(file, result);
                        }
                    }
                });
                return;
            }

            // 归一化说话人标签（占位名），完整保留所有说话人，不折叠。
            // 业务角色映射（如「来访者 / 咨询师」）由模块级监听器完成，
            // 宿主不内置任何业务分类。
            result.alignSpeakerLabels();

            // 执行分析
            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    if (AudioUnitMeta.this.sentiment) {
                        VoiceDiarizationIndicator voiceIndicator = new VoiceDiarizationIndicator(result.getId());
                        voiceIndicator.analyse(AudioUnitMeta.this.service.getCellet().getAIGCHost(), result);

                        // 设置指标
                        result.indicator = voiceIndicator;
                    }

                    // 优化标签名称
                    result.enhanceSpeakerDisplayNames(Language.Chinese);

                    // 模块级监听器扇出：先于请求级回调，模块可在此做
                    // 角色映射等收尾；其改写会被请求级回调与落库看到
                    AudioUnitMeta.this.service.notifyDiarizationListeners(file, result);

                    if (null != voiceDiarizationListener) {
                        voiceDiarizationListener.onCompleted(file, result);
                    }

                    if (storage) {
                        service.getStorage().writeVoiceDiarization(result);
                    }
                }
            });
        }
        else {
            Logger.e(this.getClass(), "#process - Unknown action: " + this.action.name);
        }
    }
}
