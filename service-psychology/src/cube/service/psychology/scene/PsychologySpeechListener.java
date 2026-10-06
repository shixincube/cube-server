/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.psychology.scene;

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.listener.SpeechModuleListener;
import cube.aigc.psychology.Role;
import cube.aigc.spi.AIGCHost;
import cube.common.entity.AIGCUnit;
import cube.common.entity.FileLabel;
import cube.common.entity.GeneratingOption;
import cube.common.entity.GeneratingRecord;
import cube.common.entity.VoiceDiarization;
import cube.common.entity.VoiceTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 心理学模块的语音监听器。
 *
 * <p><b>职责</b>：订阅宿主的「说话人分离完成」事件，把分离结果中的说话人
 * 映射为咨询业务角色——{@link Role#Counselor}（咨询师）与
 * {@link Role#Customer}（来访者）。宿主只产出完整、中性的说话人集合，
 * 角色映射属咨询业务的收尾操作，故在本模块内完成。</p>
 *
 * <p><b>迁移来源</b>：本逻辑原硬编码于宿主 {@code AudioUnitMeta}（含
 * {@code Role} 依赖），插件化后下沉至此。提示词与解析规则逐字保留，
 * 仅把「宿主服务直调」改为经 {@link AIGCHost} 转发：</p>
 * <ul>
 *     <li>文本生成：{@code service.syncGenerateText} →
 *     {@code host.selectUnit + host.syncGenerateText}；</li>
 *     <li>应答分词：{@code service.getTokenizer().sentenceProcess} →
 *     {@code host.tokenize}（同为分词结果列表，仅切分模式略有差异，
 *     对该模糊解析无影响）。</li>
 * </ul>
 *
 * <p><b>兜底</b>：无可用文本单元、生成失败或解析不出角色时，退回
 * 「首个说话人视为咨询师、其余视为来访者」的猜测规则（与原宿主实现
 * {@code guessSpeakerLabels} 逐字一致，该方法已随迁移从公共实体删除）。</p>
 *
 * <p><b>线程</b>：回调在宿主的分离收尾线程上触发；实现内无共享可变状态，
 * 线程安全。</p>
 */
public class PsychologySpeechListener implements SpeechModuleListener {

    /**
     * 监听器名称。
     */
    public final static String NAME = "psychology-speech";

    private final static PsychologySpeechListener instance = new PsychologySpeechListener();

    private AIGCHost host;

    private PsychologySpeechListener() {
    }

    public static PsychologySpeechListener getInstance() {
        return instance;
    }

    /**
     * 装配宿主能力并注册到宿主的语音事件扇出链。
     *
     * @param host 宿主能力接口。
     */
    public void setup(AIGCHost host) {
        this.host = host;
        host.registerSpeechListener(this);
    }

    /**
     * 从宿主扇出链注销并断开宿主引用。
     *
     * <p>模块卸载与装载回滚时调用。</p>
     */
    public void teardown() {
        if (null != this.host) {
            try {
                this.host.unregisterSpeechListener(this);
            } catch (Throwable t) {
                Logger.e(this.getClass(), "#teardown - Unregister FAILED",
                        (t instanceof Exception) ? (Exception) t : null);
            }
        }

        this.host = null;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void onDiarizationCompleted(FileLabel source, VoiceDiarization diarization) {
        if (null == this.host || null == diarization || diarization.tracks.isEmpty()) {
            return;
        }

        // 分类说话人角色：咨询师 / 来访者（客户）
        String prompt = this.makeClassifyPrompt(diarization);
        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#onDiarizationCompleted - Classify prompt: " + prompt);
        }

        AIGCUnit unit = this.host.selectUnit(ModelConfig.BAIZE_UNIT);
        GeneratingRecord record = (null == unit) ? null
                : this.host.syncGenerateText(unit, prompt, new GeneratingOption(), null, null);
        if (null == record) {
            this.guessSpeakerLabels(diarization);
            return;
        }

        Map<String, String> nameMap = this.analyseSpeakerClassify(record.answer, diarization);
        if (nameMap.isEmpty()) {
            this.guessSpeakerLabels(diarization);
        }
        else {
            for (Map.Entry<String, String> entry : nameMap.entrySet()) {
                diarization.setTrackLabel(entry.getKey(), entry.getValue());
                Logger.d(this.getClass(), "#onDiarizationCompleted - Classify - "
                        + entry.getKey() + " -> " + entry.getValue());
            }
        }
    }

    /**
     * 构建角色分类提示词。
     *
     * <p>先对齐说话人标签（占位名），再取首、中、尾各若干段对话作为判断依据。
     * 与原宿主实现逐字一致。</p>
     *
     * @param diarization 分离结果。
     * @return 返回提示词。
     */
    private String makeClassifyPrompt(VoiceDiarization diarization) {
        // 对齐标签
        int numSpeakers = diarization.alignSpeakerLabels();
        List<String> speakerNames = diarization.extractTrackLabels();
        int totalTracks = diarization.tracks.size();

        StringBuffer buf = new StringBuffer();
        if (speakerNames.size() == 1) {
            buf.append("已知");
            buf.append(speakerNames.get(0));
            buf.append("所说内容如下：\n\n");
            List<Integer> indexes = parseTrackIndexes(totalTracks);
            for (Integer index : indexes) {
                VoiceTrack track = diarization.tracks.get(index);
                buf.append(track.recognition.text).append("\n");
            }
            buf.append("\n根据以上内容判断");
            buf.append(speakerNames.get(0));
            buf.append("是心理咨询师还是心理咨询客户。如果是心理咨询师回复：“");
            buf.append(speakerNames.get(0));
            buf.append("是心理咨询师”，如果是心理咨询客户回复：“");
            buf.append(speakerNames.get(0));
            buf.append("是客户”。");
        }
        else {
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
        }
        return buf.toString();
    }

    /**
     * 解析可用的对话索引。
     *
     * @param length 轨迹总数。
     * @return 返回参与判断的轨迹索引。
     */
    private List<Integer> parseTrackIndexes(int length) {
        List<Integer> result = new ArrayList<>();
        if (length <= 20) {
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

    /**
     * 解析分类应答，得到「说话人 → 角色」映射。
     *
     * @param answer 文本单元的分类应答。
     * @param diarization 分离结果。
     * @return 返回映射；解析不出任何角色时返回空映射。
     */
    private Map<String, String> analyseSpeakerClassify(String answer, VoiceDiarization diarization) {
        int numSpeakers = diarization.extractTrackLabels().size();

        Map<String, String> result = new HashMap<>();
        // 分词
        List<String> words = this.host.tokenize(answer);
        for (String name : VoiceDiarization.SPEAKER_NAMES) {
            for (int i = 0; i < words.size(); ++i) {
                String word = words.get(i);
                if (word.contains(name)) {
                    // 找到名字，向后查找词
                    for (int p = i + 1; p < words.size(); ++p) {
                        String next = words.get(p);
                        if (next.contains("咨询师")) {
                            result.put(name, Role.Counselor.label);
                            break;
                        }
                        else if (next.contains("客户")) {
                            result.put(name, Role.Customer.label);
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

    /**
     * 猜测说话人角色：单人视为咨询师；多人时首个说话人视为咨询师，
     * 其余视为来访者。
     *
     * @param diarization 分离结果。
     */
    private void guessSpeakerLabels(VoiceDiarization diarization) {
        HashMap<String, List<VoiceTrack>> map = new HashMap<>();
        for (int i = 0; i < diarization.tracks.size(); ++i) {
            VoiceTrack track = diarization.tracks.get(i);
            track.track = String.valueOf(i + 1);

            if (map.containsKey(track.label)) {
                List<VoiceTrack> trackList = map.get(track.label);
                trackList.add(track);
            }
            else {
                List<VoiceTrack> trackList = new ArrayList<>();
                trackList.add(track);
                map.put(track.label, trackList);
            }
        }

        if (map.size() == 1) {
            for (VoiceTrack track : diarization.tracks) {
                track.label = Role.Counselor.label;
            }
        }
        else if (map.size() >= 2) {
            VoiceTrack first = diarization.tracks.get(0);
            String firstLabel = first.label;
            String otherLabel = "";
            for (VoiceTrack track : diarization.tracks) {
                if (!track.label.equals(firstLabel)) {
                    otherLabel = track.label;
                    break;
                }
            }

            for (VoiceTrack track : diarization.tracks) {
                if (track.label.equals(firstLabel)) {
                    track.label = "counselor";
                }
                else if (track.label.equals(otherLabel)) {
                    track.label = "customer";
                }
            }
        }
    }
}
