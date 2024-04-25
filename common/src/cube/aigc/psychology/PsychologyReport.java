/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.aigc.psychology;

import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.psychology.algorithm.MBTIFeature;
import cube.aigc.psychology.algorithm.Representation;
import cube.aigc.psychology.composition.EvaluationScore;
import cube.common.JSONable;
import cube.common.entity.FileLabel;
import cube.common.state.AIGCStateCode;
import cube.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 心理学报告。
 */
public class PsychologyReport implements JSONable {

    public final long sn;

    public final long contactId;

    public final long timestamp;

    private String name;

    private Attribute attribute;

    private FileLabel fileLabel;

    private String fileCode;

    private Theme theme;

    private boolean finished = false;

    private long finishedTimestamp;

    private AIGCStateCode state;

    private String markdown = null;

    private EvaluationReport evaluationReport;

    private MBTIFeature mbtiFeature;

    private List<String> behaviorList;

    private List<String> reportTextList;

    private List<ReportParagraph> paragraphList;

    public PsychologyReport(long contactId, Attribute attribute, FileLabel fileLabel, Theme theme) {
        this.sn = Utils.generateSerialNumber();
        this.contactId = contactId;
        this.timestamp = System.currentTimeMillis();
        this.name = theme.name + "-" + Utils.gsDateFormat.format(new Date(this.timestamp));
        this.attribute = attribute;
        this.fileLabel = fileLabel;
        this.theme = theme;
        this.state = AIGCStateCode.Processing;
    }

    public PsychologyReport(long sn, long contactId, long timestamp, String name, Attribute attribute,
                            String fileCode, Theme theme, long finishedTimestamp) {
        this.sn = sn;
        this.contactId = contactId;
        this.timestamp = timestamp;
        this.name = name;
        this.attribute = attribute;
        this.fileCode = fileCode;
        this.theme = theme;
        this.finished = true;
        this.finishedTimestamp = finishedTimestamp;
        this.state = AIGCStateCode.Ok;
    }

    public PsychologyReport(JSONObject json) {
        this.sn = json.getLong("sn");
        this.contactId = json.getLong("contactId");
        this.timestamp = json.getLong("timestamp");
        this.name = json.getString("name");
        this.attribute = new Attribute(json.getJSONObject("attribute"));

        if (json.has("fileLabel")) {
            this.fileLabel = new FileLabel(json.getJSONObject("fileLabel"));
        }
        else if (json.has("fileCode")) {
            this.fileCode = json.getString("fileCode");
        }

        this.theme = Theme.parse(json.getString("theme"));
        this.finished = json.getBoolean("finished");
        this.finishedTimestamp = json.getLong("finishedTimestamp");
        this.state = AIGCStateCode.parse(json.getInt("state"));

        if (json.has("markdown")) {
            this.markdown = json.getString("markdown");
        }

        if (json.has("mbti")) {
            this.mbtiFeature = new MBTIFeature(json.getJSONObject("mbti"));
        }

        if (json.has("evaluation")) {
            this.evaluationReport = new EvaluationReport(json.getJSONObject("evaluation"));
        }

        if (json.has("behaviorList")) {
            this.behaviorList = JSONUtils.toStringList(json.getJSONArray("behaviorList"));
        }

        if (json.has("reportTextList")) {
            this.reportTextList = JSONUtils.toStringList(json.getJSONArray("reportTextList"));
        }

        if (json.has("paragraphList")) {
            this.paragraphList = new ArrayList<>();
            JSONArray array = json.getJSONArray("paragraphList");
            for (int i = 0; i < array.length(); ++i) {
                this.paragraphList.add(new ReportParagraph(array.getJSONObject(i)));
            }
        }
    }

    public void setEvaluationReport(EvaluationReport evaluationReport) {
        this.evaluationReport = evaluationReport;
    }

    public EvaluationReport getEvaluationReport() {
        return this.evaluationReport;
    }

    public void setMBTIFeature(MBTIFeature mbtiFeature) {
        this.mbtiFeature = mbtiFeature;
    }

    public MBTIFeature getMBTIFeature() {
        return this.mbtiFeature;
    }

    public void setBehaviorList(List<String> behaviorList) {
        this.behaviorList = new ArrayList<>();
        this.behaviorList.addAll(behaviorList);
    }

    public List<String> getBehaviorList() {
        return this.behaviorList;
    }

    public void setReportTextList(List<String> textList) {
        this.reportTextList = new ArrayList<>();
        this.reportTextList.addAll(textList);
    }

    public List<String> getReportTextList() {
        return this.reportTextList;
    }

    public String getName() {
        return this.name;
    }

    public Attribute getAttribute() {
        return this.attribute;
    }

    public Theme getTheme() {
        return this.theme;
    }

    public void setState(AIGCStateCode state) {
        this.state = state;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
        this.finishedTimestamp = System.currentTimeMillis();
    }

    public long getFinishedTimestamp() {
        return this.finishedTimestamp;
    }

    public FileLabel getFileLabel() {
        return this.fileLabel;
    }

    public void setFileLabel(FileLabel fileLabel) {
        this.fileLabel = fileLabel;
    }

    public String getFileCode() {
        return (null != this.fileCode) ? this.fileCode : this.fileLabel.getFileCode();
    }

    public List<ReportParagraph> getParagraphs() {
        return this.paragraphList;
    }

    public void setParagraphs(List<ReportParagraph> list) {
        this.paragraphList = new ArrayList<>();
        this.paragraphList.addAll(list);
    }

    public void addParagraph(ReportParagraph paragraph) {
        if (null == this.paragraphList) {
            this.paragraphList = new ArrayList<>();
        }
        this.paragraphList.add(paragraph);
    }

    public boolean isNull() {
        if (null == this.evaluationReport) {
            Logger.d(this.getClass(), "Data is null");
        }

        return (null == this.evaluationReport);
    }

    public String makeMarkdown(boolean outputParagraph) {
        StringBuilder buf = new StringBuilder();
        buf.append("# ").append(this.theme.name).append("报告");

        buf.append("\n\n");
        buf.append("> ").append(this.attribute.getGenderText());
        buf.append("    ").append(this.attribute.getAgeText());
        buf.append("\n> ").append(Utils.gsDateFormat.format(new Date(this.timestamp))).append("\n");

        if (null != this.evaluationReport) {
            if (this.evaluationReport.numRepresentations() > 0) {
                buf.append("\n\n");
                buf.append("**特征表**");
                buf.append("\n\n");
                buf.append("| 特征 | 描述 | 正向趋势 | 负向趋势 |");
                buf.append("\n");
                buf.append("| ---- | ---- | ---- | ---- |");
                for (Representation rep : this.evaluationReport.getRepresentationListByEvaluationScore()) {
                    buf.append("\n");
                    buf.append("|").append(rep.knowledgeStrategy.getComment().word);
                    buf.append("|").append(rep.description);
                    buf.append("|").append(rep.positiveCorrelation);
                    buf.append("|").append(rep.negativeCorrelation);
                    buf.append("|");
                }
                buf.append("\n");
            }

            if (this.evaluationReport.numEvaluationScores() > 0) {
                buf.append("\n");
                buf.append("**评分表**");
                buf.append("\n\n");
                buf.append("| 评分项目 | 计分 | 计数 | 正权重分 | 负权重分 |");
                buf.append("\n");
                buf.append("| ---- | ---- | ---- | ---- | ---- |");
                for (EvaluationScore score : this.evaluationReport.getEvaluationScores()) {
                    buf.append("\n");
                    buf.append("|").append(score.indicator.name);
                    buf.append("|").append(score.value);
                    buf.append("|").append(score.hit);
                    buf.append("|").append(score.positiveScore);
                    buf.append("|").append(score.negativeScore);
                    buf.append("|");
                }
                buf.append("\n");
            }

            // 心理状态是否异常需要关注
            buf.append("\n");
            buf.append("**是否需要关注**");
            buf.append("\n\n");
            buf.append(this.evaluationReport.getAttentionSuggestion().description);
            buf.append("\n");
        }

        if (null != this.mbtiFeature) {
            buf.append("\n\n");
            buf.append("**MBTI 性格倾向**");
            buf.append("\n\n");
            buf.append("- **性格类型** ：").append(this.mbtiFeature.getName())
                    .append(" （").append(this.mbtiFeature.getCode()).append("）");
            buf.append("\n\n");
            buf.append("- **性格描述** ：").append(this.mbtiFeature.getDescription());
            buf.append("\n\n");
        }

        if (null != this.reportTextList) {
            buf.append("\n");
            buf.append("**报告文本：**");
            buf.append("\n");
            for (String text : this.reportTextList) {
                buf.append("\n");
                buf.append("> ").append(text);
                buf.append("\n\n");
            }
            buf.append("\n\n");
        }

        if (null != this.behaviorList) {
            buf.append("\n");
            buf.append("**行为特征：**");
            buf.append("\n");
            for (String behavior : this.behaviorList) {
                buf.append("\n");

                String[] lines = behavior.split("\n");
                for (String line : lines) {
                    if (line.trim().length() <= 2) {
                        continue;
                    }

                    buf.append("> ").append(line);
                    buf.append("\n");
                }

                buf.append("\n***\n");
            }
            buf.append("\n");
        }

        if (outputParagraph && null != this.paragraphList) {
            for (ReportParagraph paragraph : this.paragraphList) {
                buf.append(paragraph.markdown(true));
            }
        }

        this.markdown = buf.toString();
        return this.markdown;
    }

    public JSONObject toJSON(boolean markdown) {
        JSONObject json = this.toJSON();
        if (!markdown) {
            json.remove("markdown");
        }
        return json;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = this.toCompactJSON();

        if (null != this.evaluationReport) {
            json.put("evaluation", this.evaluationReport.toCompactJSON());
        }

        if (null != this.behaviorList) {
            json.put("behaviorList", JSONUtils.toStringArray(this.behaviorList));
        }

        if (null != this.reportTextList) {
            json.put("reportTextList", JSONUtils.toStringArray(this.reportTextList));
        }

        if (null != this.paragraphList) {
            JSONArray array = new JSONArray();
            for (ReportParagraph paragraph : this.paragraphList) {
                array.put(paragraph.toJSON());
            }
            json.put("paragraphList", array);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("contactId", this.contactId);
        json.put("name", this.name);
        json.put("attribute", this.attribute.toJSON());

        if (null != this.fileLabel) {
            json.put("fileLabel", this.fileLabel.toCompactJSON());
        }
        else if (null != this.fileCode) {
            json.put("fileCode", this.fileCode);
        }

        json.put("theme", this.theme.name);
        json.put("timestamp", this.timestamp);
        json.put("finished", this.finished);
        json.put("finishedTimestamp", this.finishedTimestamp);
        json.put("state", this.state.code);

        if (null != this.markdown) {
            json.put("markdown", this.markdown);
        }

        if (null != this.mbtiFeature) {
            json.put("mbti", this.mbtiFeature.toJSON());
        }

        if (null != this.evaluationReport) {
            JSONObject attention = new JSONObject();
            attention.put("level", this.evaluationReport.getAttentionSuggestion().level);
            attention.put("desc", this.evaluationReport.getAttentionSuggestion().description);
            json.put("attention", attention);
        }

        return json;
    }
}
