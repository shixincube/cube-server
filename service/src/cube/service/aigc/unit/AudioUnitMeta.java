/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.unit;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.auth.AuthToken;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.VoiceDiarizationListener;
import cube.service.aigc.scene.VoiceDiarizationIndicator;
import cube.util.FileUtils;
import cube.util.TimeUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AudioUnitMeta extends UnitMeta {

    protected AuthToken authToken;

    protected FileLabel file;

    protected AIGCAction action;

    public VoiceDiarizationListener voiceDiarizationListener;

    public AudioUnitMeta(AIGCService service, AIGCUnit unit, AuthToken authToken, AIGCAction action, FileLabel file) {
        super(service, unit);
        this.authToken = authToken;
        this.action = action;
        this.file = file;
    }

    @Override
    public void process() {
        if (null == this.voiceDiarizationListener) {
            Logger.w(this.getClass(), "#process - The listener is null: " + this.file.getFileCode());
        }

        JSONObject data = new JSONObject();
        data.put("fileLabel", this.file.toJSON());
        Packet request = new Packet(this.action.name, data);
        ActionDialect dialect = this.service.getCellet().transmit(this.unit.getContext(), request.toDialect(), 5 * 60 * 1000);
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
        if (this.action == AIGCAction.SpeakerDiarization) {
            VoiceDiarization result = new VoiceDiarization(payload.getJSONObject("result"));
            // 补齐参数
            result.contactId = this.authToken.getContactId();
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

            // 分析说话者
            String prompt = this.makeClassifyTrackLabelPrompt(result);
            if (Logger.isDebugLevel()) {
                Logger.d(this.getClass(), "#process - makeClassifyTrackLabelPrompt: " + prompt);
            }
            GeneratingRecord record = this.service.syncGenerateText(ModelConfig.BAIZE_NEXT_UNIT, prompt,
                    new GeneratingOption(), null, null);
            if (null == record) {
                result.guessSpeakerLabels();
            }
            else {
                Map<String, String> nameMap = this.analyseSpeakerClassify(record, result);
                if (nameMap.isEmpty()) {
                    result.guessSpeakerLabels();
                }
                else {
                    for (Map.Entry<String, String> entry : nameMap.entrySet()) {
                        result.setTrackLabel(entry.getKey(), entry.getValue());
                        Logger.d(this.getClass(), "#process - analyse speaker classify - " + entry.getKey() + " -> " + entry.getValue());
                    }
                }
            }

            // 执行分析
            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    VoiceDiarizationIndicator voiceIndicator = new VoiceDiarizationIndicator(result.getId());
                    voiceIndicator.analyse(service, result);

                    // 设置指标
                    result.indicator = voiceIndicator;

                    if (null != voiceDiarizationListener) {
                        voiceDiarizationListener.onCompleted(file, result);
                    }

                    service.getStorage().writeVoiceDiarization(result);
                }
            });
        }
        else {
            Logger.e(this.getClass(), "#process - Unknown action: " + this.action.name);
        }
    }

    private Map<String, String> analyseSpeakerClassify(GeneratingRecord record, VoiceDiarization diarization) {
        int numSpeakers = diarization.extractTrackLabels().size();

        Map<String, String> result = new HashMap<>();
        String answer = record.answer;
        // 分词
        List<String> words = this.service.getTokenizer().sentenceProcess(answer);
        for (String name : VoiceDiarization.SPEAKER_NAMES) {
            for (int i = 0; i < words.size(); ++i) {
                String word = words.get(i);
                if (word.contains(name)) {
                    // 找到名字，向后查找词
                    for (int p = i + 1; p < words.size(); ++p) {
                        String next = words.get(p);
                        if (next.contains("咨询师")) {
                            result.put(name, VoiceDiarization.LABEL_COUNSELOR);
                            break;
                        }
                        else if (next.contains("客户")) {
                            result.put(name, VoiceDiarization.LABEL_CUSTOMER);
                            break;
                        }
                    }
                }
            }

            if (result.size() == numSpeakers) {
                break;
            }
        }
        return result;
    }

    private String makeClassifyTrackLabelPrompt(VoiceDiarization diarization) {
        // 对齐标签
        int numSpeakers = diarization.alignSpeakerLabels();
        List<String> speakerNames = diarization.extractTrackLabels();
        int totalTracks = diarization.tracks.size();

        StringBuffer buf = new StringBuffer();
        buf.append("已知");
        for (String name : speakerNames) {
            buf.append(name).append("，");
        }
        buf.delete(buf.length() - 1, buf.length());
        buf.append("等");
        buf.append(numSpeakers).append("个人的谈话内容如下：\n\n");
        List<Integer> indexes = parseTrackIndexes(totalTracks);
        for (Integer index : indexes) {
            VoiceTrack track = diarization.tracks.get(index);
            buf.append(track.label).append("说：").append(track.recognition.text).append("\n\n");
        }

        buf.append("根据以上对话内容判断");
        for (String name : speakerNames) {
            buf.append(name).append("，");
        }
        buf.delete(buf.length() - 1, buf.length());
        buf.append("等人中谁是心理咨询师，谁是心理咨询客户。使用回复格式：“XX是心理咨询师”，“YY是客户”，其中XX使用心理咨询师名字替换，YY使用客户名字替换。");
        return buf.toString();
    }

    private List<Integer> parseTrackIndexes(int length) {
        List<Integer> result = new ArrayList<>();
        if (length <= 12) {
            for (int i = 0; i < length; ++i) {
                result.add(i);
            }
        }
        else {
            int middle = (int) Math.floor(length >> 1);
            for (int i = 0; i < 4; ++i) {
                result.add(i);
            }

            for (int i = middle - 2; i < middle + 2; ++i) {
                result.add(i);
            }

            for (int i = length - 4; i < length; ++i) {
                result.add(i);
            }
        }
        return result;
    }
}
