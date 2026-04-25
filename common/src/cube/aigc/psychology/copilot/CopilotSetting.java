/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology.copilot;

import cell.util.Utils;
import cube.common.JSONable;
import cube.util.JSONUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CopilotSetting implements JSONable {

    /**
     * 人格特质
     */
    public final PersonalityTrait personalityTrait;

    /**
     * 依恋类型
     */
    public final AttachmentType attachmentType;

    /**
     * 文化背景
     */
    public final CulturalBackground culturalBackground;

    /**
     * 主诉类型
     */
    public final ChiefComplaintType chiefComplaintType;

    /**
     * 痛苦程度
     */
    public final PainLevel painLevel;

    /**
     * 防御机制
     */
    public final DefenseMechanism defenseMechanism;

    /**
     * 移情
     */
    public final Empathy empathy;

    /**
     * 言语风格
     */
    public final SpeechStyle speechStyle;

    /**
     * “隐藏议程”模式
     */
    public boolean hiddenAgendaModel;

    /**
     * “多重角色”模式
     */
    public boolean multipleRolesModel;

    /**
     * “伦理陷阱”模式
     */
    public boolean ethicalTrapModel;

    /**
     * 序号。
     */
    private long sn;

    /**
     * 策略模板。
     */
    private String strategyTemplate;

    /**
     * 可选句子。
     */
    private List<String> sentences;

    public CopilotSetting(PersonalityTrait personalityTrait, AttachmentType attachmentType,
                          CulturalBackground culturalBackground, ChiefComplaintType chiefComplaintType,
                          PainLevel painLevel, DefenseMechanism defenseMechanism,
                          Empathy empathy, SpeechStyle speechStyle,
                          boolean hiddenAgendaModel, boolean multipleRolesModel, boolean ethicalTrapModel) {
        this.personalityTrait = personalityTrait == PersonalityTrait.Random ?
                PersonalityTrait.random() : personalityTrait;
        this.attachmentType = attachmentType == AttachmentType.Random ?
                AttachmentType.random() : attachmentType;
        this.culturalBackground = culturalBackground == CulturalBackground.Random ?
                CulturalBackground.random() : culturalBackground;
        this.chiefComplaintType = chiefComplaintType == ChiefComplaintType.Random ?
                ChiefComplaintType.random() : chiefComplaintType;
        this.painLevel = painLevel == PainLevel.Random ? PainLevel.random() : painLevel;
        this.defenseMechanism = defenseMechanism == DefenseMechanism.Random ?
                DefenseMechanism.random() : defenseMechanism;
        this.empathy = empathy == Empathy.Random ? Empathy.random() : empathy;
        this.speechStyle = speechStyle == SpeechStyle.Random ? SpeechStyle.random() : speechStyle;
        this.hiddenAgendaModel = hiddenAgendaModel;
        this.multipleRolesModel = multipleRolesModel;
        this.ethicalTrapModel = ethicalTrapModel;
        this.sn = Utils.generateSerialNumber();
    }

    public CopilotSetting(JSONObject json) {
        this.personalityTrait = PersonalityTrait.parse(json.getString("personalityTrait"));
        this.attachmentType = AttachmentType.parse(json.getString("attachmentType"));
        this.culturalBackground = CulturalBackground.parse(json.getString("culturalBackground"));
        this.chiefComplaintType = ChiefComplaintType.parse(json.getString("chiefComplaintType"));
        this.painLevel = PainLevel.parse(json.getString("painLevel"));
        this.defenseMechanism = DefenseMechanism.parse(json.getString("defenseMechanism"));
        this.empathy = Empathy.parse(json.getString("empathy"));
        this.speechStyle = SpeechStyle.parse(json.getString("speechStyle"));
        this.hiddenAgendaModel = json.getBoolean("hiddenAgendaModel");
        this.multipleRolesModel = json.getBoolean("multipleRolesModel");
        this.ethicalTrapModel = json.getBoolean("ethicalTrapModel");
        if (json.has("sn")) {
            this.sn = json.getLong("sn");
        }
        if (json.has("strategyTemplate")) {
            this.strategyTemplate = json.getString("strategyTemplate");
        }
        if (json.has("sentences")) {
            this.sentences = JSONUtils.toStringList(json.getJSONArray("sentences"));
        }
    }

    public long getSn() {
        return this.sn;
    }

    public void setStrategyTemplate(String content) {
        this.strategyTemplate = content;
    }

    public String getStrategyTemplate() {
        return this.strategyTemplate;
    }

    public void addSentences(List<String> sentences) {
        if (null == this.sentences) {
            this.sentences = new ArrayList<>();
        }

        this.sentences.addAll(sentences);
    }

    public String toMarkdown() {
        StringBuilder buf = new StringBuilder();
        buf.append("* 人格特质：").append(this.personalityTrait.display)
            .append("。").append(this.personalityTrait.description).append("\n");
        buf.append("* 依恋类型：").append(this.attachmentType.display)
                .append("。").append(this.attachmentType.description).append("\n");
        buf.append("* 文化背景：").append(this.culturalBackground.display)
                .append("。").append(this.culturalBackground.description).append("\n");
        buf.append("* 主诉类型：").append(this.chiefComplaintType.display)
                .append("。").append(this.chiefComplaintType.description).append("\n");
        buf.append("* 痛苦程度：").append(this.painLevel.display)
                .append("。").append(this.painLevel.description).append("\n");
        buf.append("* 防御机制：").append(this.defenseMechanism.display)
                .append("。").append(this.defenseMechanism.description).append("\n");
        buf.append("* 移情：").append(this.empathy.display)
                .append("。").append(this.empathy.description).append("\n");
//        buf.append("* 言语风格：").append(this.speechStyle.display)
//                .append("。").append(this.speechStyle.description).append("\n");
        return buf.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("personalityTrait", this.personalityTrait.name());
        json.put("attachmentType", this.attachmentType.name());
        json.put("culturalBackground", this.culturalBackground.name());
        json.put("chiefComplaintType", this.chiefComplaintType.name());
        json.put("painLevel", this.painLevel.name());
        json.put("defenseMechanism", this.defenseMechanism.name());
        json.put("empathy", this.empathy.name());
        json.put("speechStyle", this.speechStyle.name());
        json.put("hiddenAgendaModel", this.hiddenAgendaModel);
        json.put("multipleRolesModel", this.multipleRolesModel);
        json.put("ethicalTrapModel", this.ethicalTrapModel);
        json.put("sn", this.sn);
        if (null != this.strategyTemplate) {
            json.put("strategyTemplate", this.strategyTemplate);
        }
        if (null != this.sentences) {
            json.put("sentences", JSONUtils.toStringArray(this.sentences));
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = this.toJSON();
        if (json.has("strategyTemplate")) {
            json.remove("strategyTemplate");
        }
        if (json.has("sentences")) {
            json.remove("sentences");
        }
        return json;
    }
}
