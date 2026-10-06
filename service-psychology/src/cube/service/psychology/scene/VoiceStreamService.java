/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.psychology.scene;

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.listener.VoiceDiarizationListener;
import cube.aigc.listener.VoiceStreamAnalysisListener;
import cube.aigc.spi.AIGCHost;
import cube.auth.AuthToken;
import cube.common.Language;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.service.psychology.PsychologyStorage;
import cube.util.TimeUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语音流服务。
 *
 * <p>承载「录制咨询音频 → 说话人分离 → 归档 → 停止」这条链路的宿主侧编排。
 * 链路上真正做业务的是 {@link CounselingManager}（流内存态、归档、策略生成），
 * 本类负责入站动作的协议编排：参数校验、待处理分片登记、
 * 分离结果回填、以及录音归档记录的读写。</p>
 *
 * <p><b>为何从 AIGCService 迁入本模块</b>：这三个动作（分析语音流、停止语音流、
 * 语音分析）只服务咨询业务，其状态（待处理分片表、咨询录音表）也只服务咨询业务。
 * 留在宿主会让咨询链路反向依赖宿主门面。</p>
 *
 * <p><b>仍需宿主能力的地方</b>：说话人分离（音频单元）、文件读写、
 * 文本生成、延迟任务、丢弃音频队列任务 —— 全部经 {@link AIGCHost} 转发。
 * 唯独 {@code voice_diarization} 表的写入由宿主的音频单元完成
 * （那是平台级能力，不属心理学），故本类读它需经
 * {@link AIGCHost#readVoiceDiarization}。</p>
 */
public class VoiceStreamService {

    /**
     * 停止语音流后延迟删除音频文件的时长（毫秒）。
     *
     * <p>给在途的分离任务留出收尾时间：立即删除会让正在读文件的任务失败。</p>
     */
    private final static long DELETE_DELAY = 45 * 1000L;

    /**
     * 过期未完成分片的存活上限（毫秒）。
     *
     * <p>与宿主原实现一致（4 小时）：超过即视为该流已 abandoned，
     * 残留文件需清理，否则长期运行会累积。</p>
     */
    private final static long SINK_TTL = 4 * 60 * 60 * 1000L;

    private final static VoiceStreamService instance = new VoiceStreamService();

    /**
     * Key 是 stream name，value 是已提交分离但尚未回调的分片。
     *
     * <p>与 {@link CounselingManager} 内的 {@code streamSinkMap} 语义不同：
     * 那份是「已完成分离、待生成策略」，这份是「分离在途」。</p>
     */
    private final Map<String, List<VoiceStreamSink>> waitingSinks = new ConcurrentHashMap<>();

    private AIGCHost host;

    private PsychologyStorage storage;

    private VoiceStreamService() {
    }

    public static VoiceStreamService getInstance() {
        return instance;
    }

    /**
     * 装配宿主能力与存储。
     *
     * @param host 宿主能力接口。
     * @param storage 模块私有存储。
     */
    public void setup(AIGCHost host, PsychologyStorage storage) {
        this.host = host;
        this.storage = storage;
    }

    /**
     * 停止并清空。
     *
     * <p>模块卸载时调用：清空待处理分片表并断开宿主引用。</p>
     */
    public void teardown() {
        this.waitingSinks.clear();
        this.host = null;
        this.storage = null;
    }

    /**
     * 心跳：清理超期未完成的分片。
     *
     * @param now 当前时刻。
     */
    public void onTick(long now) {
        if (null == this.host) {
            return;
        }

        Iterator<Map.Entry<String, List<VoiceStreamSink>>> iter = this.waitingSinks.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, List<VoiceStreamSink>> entry = iter.next();
            List<VoiceStreamSink> list = entry.getValue();
            synchronized (list) {
                Iterator<VoiceStreamSink> sinkIter = list.iterator();
                while (sinkIter.hasNext()) {
                    VoiceStreamSink sink = sinkIter.next();
                    if (now - sink.getTimestamp() > SINK_TTL) {
                        this.host.deleteFile(sink.authToken.getDomain(), sink.getFileCode());
                        sinkIter.remove();
                    }
                }
            }

            if (list.isEmpty()) {
                iter.remove();
            }
        }
    }

    // ───────── 分析语音流 ─────────

    /**
     * 分析语音流：登记分片并发起说话人分离。
     *
     * <p>分离是异步的，结果经 {@code listener} 回调；本方法在分离被受理
     * 时即返回受理结果，<b>不</b>等待分离完成。</p>
     *
     * @param authToken 访问令牌。
     * @param fileCode 分片文件码。
     * @param streamName 流名。
     * @param index 分片序号。
     * @param listener 分离结果回调。
     * @return 受理成功返回 <code>true</code>；流已超时或分离不可用返回 <code>false</code>。
     */
    public boolean analyse(AuthToken authToken, String fileCode, String streamName, int index,
            VoiceStreamAnalysisListener listener) {
        if (CounselingManager.getInstance().isOverDurationLimit(streamName)) {
            Logger.i(this.getClass(), "#analyse - Over duration limit: " + streamName);
            // 超时即直接收尾：停止该流并丢弃本分片
            this.stop(authToken, streamName);
            return false;
        }

        // 归档原始音频（拼接为整段录音）
        this.host.schedule("voice-stream-archive", 0L, () ->
                CounselingManager.getInstance().archive(authToken, fileCode, streamName, index));

        final VoiceStreamSink streamSink = new VoiceStreamSink(streamName, index, fileCode);
        streamSink.authToken = authToken;

        List<VoiceStreamSink> list = this.waitingSinks.computeIfAbsent(streamName, k -> new ArrayList<>());
        synchronized (list) {
            list.add(streamSink);
        }

        // 指标分析开启：咨询策略的情绪因子依赖分离结果里的 indicator
        FileLabel fileLabel = this.host.performSpeakerDiarization(authToken,
                this.host.getFile(authToken.getDomain(), fileCode), false, false, false, true,
                new VoiceDiarizationListener() {
                    @Override
                    public void onCompleted(FileLabel source, VoiceDiarization diarization) {
                        VoiceStreamService.this.removeWaitingSink(streamName, streamSink);

                        streamSink.setDiarization(diarization);
                        streamSink.setFileLabel(source);

                        listener.onCompleted(source, streamSink);

                        // 交咨询管理器：触发策略与字幕生成
                        CounselingManager.getInstance().record(streamSink);
                    }

                    @Override
                    public void onFailed(FileLabel source, AIGCStateCode stateCode) {
                        VoiceStreamService.this.removeWaitingSink(streamName, streamSink);
                        listener.onFailed(source, stateCode);
                    }
                });

        return (null != fileLabel);
    }

    /**
     * 停止语音流处理。停止后不可恢复。
     *
     * @param authToken 访问令牌。
     * @param streamName 流名。
     * @return 该流已有归档记录（即此前已停止过）时返回 <code>false</code>，
     *         否则返回 <code>true</code>。
     */
    public boolean stop(AuthToken authToken, String streamName) {
        if (null != this.storage && null != this.storage.readCounselingRecording(streamName)) {
            // 已有归档记录：重复停止
            return false;
        }

        // 通知咨询管理器收尾（拼接音频、转 MP3、落归档记录）
        this.host.schedule("voice-stream-stop", 0L, () ->
                CounselingManager.getInstance().stopStream(authToken, streamName));

        // 清掉尚未分离的分片
        List<VoiceStreamSink> sinkList = this.waitingSinks.remove(streamName);
        if (null != sinkList) {
            Logger.d(this.getClass(), "#stop - Waiting size: " + sinkList.size());

            List<String> fileCodes = new ArrayList<>();
            for (VoiceStreamSink sink : sinkList) {
                fileCodes.add(sink.getFileCode());
            }

            // 丢弃音频队列里对应的待处理任务，避免它去读已删除的文件
            this.host.discardAudioTasks(fileCodes);

            // 延迟删除文件：在途的分离任务需要一段时间收尾
            this.host.schedule("voice-stream-delete", DELETE_DELAY, () -> {
                for (VoiceStreamSink sink : sinkList) {
                    VoiceStreamService.this.host.deleteFile(
                            authToken.getDomain(), sink.getFileCode());
                }
            });
        }

        return true;
    }

    // ───────── 语音分析 ─────────

    /**
     * 执行语音内容分析：按模板生成分析文本并写回。
     *
     * @param authToken 访问令牌。
     * @param fileCode 音频文件码。
     * @param templateName 模板名。
     * @param parameters 自定义参数；非空时走另一条尚未实现的路径，返回 <code>null</code>。
     * @return 返回分析文本；无分离结果、模板缺失或生成失败时返回 <code>null</code>。
     */
    public String performSpeechAnalysis(AuthToken authToken, String fileCode, String templateName,
            Map<String, String> parameters) {
        VoiceDiarization voiceDiarization = this.host.readVoiceDiarization(fileCode);
        if (null == voiceDiarization) {
            Logger.w(this.getClass(), "#performSpeechAnalysis - No voice diarization: " + fileCode);
            return null;
        }

        String prompt;
        if (null == parameters || parameters.isEmpty()) {
            PromptBuilder builder = new PromptBuilder(this.host, templateName);
            builder.put("original_transcript", voiceDiarization.buildSpeechText(true));
            builder.put("interview_date",
                    TimeUtils.formatDateString(voiceDiarization.getTimestamp(), Language.Chinese));
            builder.put("interview_duration", TimeUtils.calcTimeDuration(
                    (long) (voiceDiarization.duration * 1000)).toHumanStringDHMS());
            builder.put("interview_form", "线下");
            prompt = builder.build();

            if (null == prompt) {
                Logger.w(this.getClass(),
                        "#performSpeechAnalysis - No prompt template: " + templateName);
                return null;
            }
        }
        else {
            // 自定义参数路径尚未实现，保持既有行为：直接返回失败
            return null;
        }

        GeneratingRecord result = this.host.syncGenerateText(authToken, ModelConfig.BAIZE_2_UNIT,
                prompt, new GeneratingOption(), null, null);
        if (null == result) {
            Logger.w(this.getClass(), "#performSpeechAnalysis - Generates failed: " + fileCode);
            return null;
        }

        // 按模板回填到不同字段，再落库
        if (templateName.equalsIgnoreCase("psy_supervise_record")) {
            voiceDiarization.suggestion = result.answer;
        }
        else {
            voiceDiarization.analysis = result.answer;
        }

        final VoiceDiarization target = voiceDiarization;
        final String template = templateName;
        this.host.schedule("speech-analysis-write", 0L, () -> {
            if (template.equalsIgnoreCase("psy_organize_record")) {
                VoiceStreamService.this.host.updateVoiceDiarizationAnalysis(target);
            }
            else if (template.equalsIgnoreCase("psy_supervise_record")) {
                VoiceStreamService.this.host.updateVoiceDiarizationSuggestion(target);
            }
            else {
                VoiceStreamService.this.host.updateVoiceDiarizationAnalysis(target);
            }
        });

        return result.answer;
    }

    // ───────── 录音归档查询 ─────────

    /**
     * 按流名读取咨询录音记录。
     *
     * @param streamName 流名。
     * @return 返回记录 JSON；无记录时返回 <code>null</code>。
     */
    public JSONObject readRecording(String streamName) {
        return (null == this.storage) ? null : this.storage.readCounselingRecording(streamName);
    }

    /**
     * 按流名读取咨询录音的文件码。
     *
     * @param streamName 流名。
     * @return 返回文件码；无记录时返回 <code>null</code>。
     */
    public String readRecordingFileCode(String streamName) {
        return (null == this.storage) ? null
                : this.storage.readCounselingRecordingFileCode(streamName);
    }

    /**
     * 移除一个已完成的分片。
     *
     * @param streamName 流名。
     * @param sink 分片。
     */
    private void removeWaitingSink(String streamName, VoiceStreamSink sink) {
        List<VoiceStreamSink> list = this.waitingSinks.get(streamName);
        if (null == list) {
            return;
        }

        synchronized (list) {
            list.remove(sink);
        }
    }
}
