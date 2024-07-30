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
import cube.aigc.psychology.algorithm.BigFiveFeature;
import cube.aigc.psychology.algorithm.MBTIFeature;
import cube.aigc.psychology.algorithm.Representation;
import cube.aigc.psychology.composition.EvaluationScore;
import cube.aigc.psychology.composition.ReportSection;
import cube.aigc.psychology.composition.SixDimension;
import cube.aigc.psychology.composition.SixDimensionScore;
import cube.common.entity.FileLabel;
import cube.common.state.AIGCStateCode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 心理学绘画报告。
 */
public class PaintingReport extends Report {

    private FileLabel fileLabel;

    private String fileCode;

    private Theme theme;

    private boolean finished = false;

    private long finishedTimestamp;

    private AIGCStateCode state;

    private String markdown = null;

    private EvaluationReport evaluationReport;

    private SixDimensionScore dimensionScore;

    private SixDimensionScore normDimensionScore;

    private List<ReportSection> reportTextList;

    public PaintingReport(long contactId, Attribute attribute, FileLabel fileLabel, Theme theme) {
        super(contactId, attribute);
        this.fileLabel = fileLabel;
        this.theme = theme;
        this.state = AIGCStateCode.Processing;
    }

    public PaintingReport(long sn, long contactId, long timestamp, String name, Attribute attribute,
                          String fileCode, Theme theme, long finishedTimestamp) {
        super(sn, contactId, timestamp, name, attribute);
        this.fileCode = fileCode;
        this.theme = theme;
        this.finished = true;
        this.finishedTimestamp = finishedTimestamp;
        this.state = AIGCStateCode.Ok;
    }

    public PaintingReport(JSONObject json) {
        super(json);

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

        if (json.has("dimensionScore")) {
            this.dimensionScore = new SixDimensionScore(json.getJSONObject("dimensionScore"));
        }
        if (json.has("normDimensionScore")) {
            this.normDimensionScore = new SixDimensionScore(json.getJSONObject("normDimensionScore"));
        }

        if (json.has("evaluation")) {
            this.evaluationReport = new EvaluationReport(json.getJSONObject("evaluation"));
        }

        if (json.has("reportTextList")) {
            this.reportTextList = new ArrayList<>();
            JSONArray array = json.getJSONArray("reportTextList");
            for (int i = 0; i < array.length(); ++i) {
                ReportSection rs = new ReportSection(array.getJSONObject(i));
                this.reportTextList.add(rs);
            }
        }
    }

    public void setEvaluationReport(EvaluationReport evaluationReport) {
        this.evaluationReport = evaluationReport;
    }

    public EvaluationReport getEvaluationReport() {
        return this.evaluationReport;
    }

    public void setDimensionalScore(SixDimensionScore score, SixDimensionScore normScore) {
        this.dimensionScore = score;
        this.normDimensionScore = normScore;
    }

    public void setReportTextList(List<ReportSection> textList) {
        this.reportTextList = new ArrayList<>();
        this.reportTextList.addAll(textList);
    }

    public List<ReportSection> getReportTextList() {
        return this.reportTextList;
    }

    public Theme getTheme() {
        return this.theme;
    }

    public void setState(AIGCStateCode state) {
        this.state = state;
    }

    public AIGCStateCode getState() {
        return this.state;
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

    public boolean isNull() {
        if (null == this.evaluationReport) {
            Logger.d(this.getClass(), "Data is null");
        }

        return (null == this.evaluationReport);
    }

    public String makeMarkdown() {
        StringBuilder buf = new StringBuilder();
        buf.append("# ").append(this.theme.name).append("报告");

        buf.append("\n\n");
        buf.append("> ").append(this.attribute.getGenderText());
        buf.append("    ").append(this.attribute.getAgeText());
        buf.append("\n> ").append(Utils.gsDateFormat.format(new Date(this.timestamp))).append("\n");

        if (null != this.summary) {
            buf.append("\n\n");
            buf.append("**概述**\n");
            buf.append(this.summary);
            buf.append("\n");
        }

        if (null != this.evaluationReport) {
            buf.append("\n\n");
            buf.append("**Hesitating**：");
            buf.append(this.evaluationReport.isHesitating() ? "***是***" : "***否***");
            buf.append("\n");

            if (this.evaluationReport.numRepresentations() > 0) {
                buf.append("\n\n");
                buf.append("**特征表**");
                buf.append("\n\n");
                buf.append("| 特征 | 描述 | 正向趋势 | 负向趋势 |");
                buf.append("\n");
                buf.append("| ---- | ---- | ---- | ---- |");
                for (Representation rep : this.evaluationReport.getRepresentationListByEvaluationScore(100)) {
                    buf.append("\n");
                    buf.append("|").append(rep.knowledgeStrategy.getTerm().word);
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

        if (null != this.dimensionScore && null != this.normDimensionScore) {
            buf.append("\n");
            buf.append("**六维评价**\n\n");
            buf.append("| 维度 | 得分 | 标准分 |");
            buf.append("\n");
            buf.append("| ---- | ---- | ---- |");
            for (SixDimension dimension : SixDimension.values()) {
                buf.append("\n");
                buf.append("|").append(dimension.displayName);
                buf.append("|").append(this.dimensionScore.getDimensionScore(dimension));
                buf.append("|").append(this.normDimensionScore.getDimensionScore(dimension));
                buf.append("|");
            }
            buf.append("\n");
        }

        if (null != this.evaluationReport && null != this.evaluationReport.getPersonalityAccelerator()) {
            BigFiveFeature bigFiveFeature = this.evaluationReport.getPersonalityAccelerator().getBigFiveFeature();
            buf.append("\n\n");
            buf.append("**大五人格**");
            buf.append("\n\n");
            buf.append("- **人格画像** ：").append(bigFiveFeature.getDisplayName());
            buf.append("\n\n");
            buf.append("- **人格描述** ：").append(bigFiveFeature.getDescription());
            buf.append("\n\n");

            MBTIFeature mbtiFeature = this.evaluationReport.getPersonalityAccelerator().getMBTIFeature();
            buf.append("\n\n");
            buf.append("**MBTI 性格**");
            buf.append("\n\n");
            buf.append("- **性格类型** ：").append(mbtiFeature.getName())
                    .append(" （").append(mbtiFeature.getCode()).append("）");
            buf.append("\n\n");
            buf.append("- **性格描述** ：").append(mbtiFeature.getDescription());
            buf.append("\n\n");
        }

        if (null != this.reportTextList) {
            buf.append("\n");
            buf.append("**报告文本：**");
            buf.append("\n");
            for (ReportSection rs : this.reportTextList) {
                buf.append("\n");
                buf.append("> **").append(rs.title).append("** \n");
                buf.append("> **报告**：").append(rs.report).append("\n");
                buf.append("> **建议**：\n").append(rs.suggestion).append("\n");
                buf.append("\n***\n");
            }
            buf.append("\n\n");
        }

        this.markdown = buf.toString();
        return this.markdown;
    }

    public JSONObject toMarkdown() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        if (null != this.markdown) {
            json.put("markdown", this.markdown);
        }
        return json;
    }

    public void extendMarkdown(JSONObject json) {
        if (json.has("markdown")) {
            this.markdown = json.getString("markdown");
        }
    }

    public JSONObject toTextListJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);

        if (null != this.reportTextList) {
            JSONArray jsonArray = new JSONArray();
            for (ReportSection rs : this.reportTextList) {
                jsonArray.put(rs.toJSON());
            }
            json.put("reportTextList", jsonArray);
        }

        return json;
    }

    public void extendTextList(JSONObject json) {
        if (json.has("reportTextList")) {
            this.reportTextList = new ArrayList<>();
            JSONArray array = json.getJSONArray("reportTextList");
            for (int i = 0; i < array.length(); ++i) {
                ReportSection rs = new ReportSection(array.getJSONObject(i));
                this.reportTextList.add(rs);
            }
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = this.toCompactJSON();

        if (null != this.markdown) {
            json.put("markdown", this.markdown);
        }

        if (null != this.reportTextList) {
            JSONArray jsonArray = new JSONArray();
            for (ReportSection rs : this.reportTextList) {
                jsonArray.put(rs.toJSON());
            }
            json.put("reportTextList", jsonArray);
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toJSON();
        if (null != this.fileLabel) {
            json.put("fileLabel", this.fileLabel.toCompactJSON());
        }
        else if (null != this.fileCode) {
            json.put("fileCode", this.fileCode);
        }

        json.put("theme", this.theme.name);

        json.put("finished", this.finished);
        json.put("finishedTimestamp", this.finishedTimestamp);
        json.put("state", this.state.code);

        if (null != this.dimensionScore) {
            json.put("dimensionScore", this.dimensionScore.toJSON());
        }
        if (null != this.normDimensionScore) {
            json.put("normDimensionScore", this.normDimensionScore.toJSON());
        }

        if (null != this.evaluationReport) {
            JSONObject attention = new JSONObject();
            attention.put("level", this.evaluationReport.getAttentionSuggestion().level);
            attention.put("desc", this.evaluationReport.getAttentionSuggestion().description);
            json.put("attention", attention);

            json.put("evaluation", this.evaluationReport.toCompactJSON());

            if (null != this.evaluationReport.getPersonalityAccelerator()) {
                BigFiveFeature bigFiveFeature = this.evaluationReport.getPersonalityAccelerator().getBigFiveFeature();
                MBTIFeature mbtiFeature = this.evaluationReport.getPersonalityAccelerator().getMBTIFeature();
                JSONObject personality = new JSONObject();
                personality.put("theBigFive", bigFiveFeature.toJSON());
                personality.put("mbti", mbtiFeature.toJSON());
                json.put("personality", personality);
            }
        }
        return json;
    }
}
