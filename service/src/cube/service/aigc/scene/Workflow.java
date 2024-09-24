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

package cube.service.aigc.scene;

import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.*;
import cube.aigc.psychology.algorithm.BigFiveFeature;
import cube.aigc.psychology.algorithm.PersonalityAccelerator;
import cube.aigc.psychology.algorithm.Representation;
import cube.aigc.psychology.composition.*;
import cube.common.entity.AIGCChannel;
import cube.common.entity.GenerativeOption;
import cube.service.aigc.AIGCService;
import cube.service.tokenizer.keyword.Keyword;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;
import cube.util.FileUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流。
 */
public class Workflow {

    public final static String HighTrick = "明显";
    public final static String NormalTrick = "具有";
    public final static String LowTrick = "缺乏";//"不足";

    private final static String PERSONALITY_FORMAT = "已知信息：\n%s\n\n根据上述信息，回答问题：总结他的性格特点。";

    private boolean speed = true;

    private EvaluationReport evaluationReport;

    private AIGCChannel channel;

    private AIGCService service;

    private HexagonDimensionScore dimensionScore;

    private HexagonDimensionScore normDimensionScore;

    private List<ReportSection> reportTextList;

    private String summary = "";

    private DaturaFlower daturaFlower = new DaturaFlower("AS_001");

    private String unitName = ModelConfig.BAIZE_UNIT;

    public Workflow(AIGCService service) {
        this.service = service;
    }

    public Workflow(EvaluationReport evaluationReport, AIGCChannel channel, AIGCService service) {
        this.evaluationReport = evaluationReport;
        this.channel = channel;
        this.service = service;
        this.reportTextList = new ArrayList<>();
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public boolean isSpeed() {
        return this.speed;
    }

    public PaintingReport fillReport(PaintingReport report) {
        report.setEvaluationReport(this.evaluationReport);

        if (null != this.dimensionScore && null != this.normDimensionScore) {
            report.setDimensionalScore(this.dimensionScore, this.normDimensionScore);
        }

        report.setSummary(this.summary);
        report.setReportTextList(this.reportTextList);
        report.setDaturaFlower(this.daturaFlower);

        return report;
    }

    public boolean isUnknown() {
        return this.evaluationReport.isUnknown();
    }

    public Workflow make(Theme theme, int maxIndicatorTexts) {
        int age = this.evaluationReport.getAttribute().age;
        String gender = this.evaluationReport.getAttribute().gender;

        // 六维得分计算
        try {
            this.dimensionScore = Resource.getInstance()
                    .getHexDimProjection().calc(this.evaluationReport.getEvaluationScores());

            List<EvaluationScore> scoreList = Resource.getInstance().getBenchmark().getEvaluationScores(age);
            scoreList = this.filter(this.evaluationReport.getEvaluationScores(), scoreList);
            this.normDimensionScore = Resource.getInstance().getHexDimProjection().calc(scoreList);

            // 校准视觉效果
            for (HexagonDimension dim : HexagonDimension.values()) {
                int score = this.dimensionScore.getDimensionScore(dim);
                if (score < 10) {
                    this.dimensionScore.record(dim, (int) Math.round(score * 2.5));
                }
                else if (score < 20) {
                    this.dimensionScore.record(dim, (int) Math.round(score * 2));
                }

                int norm = this.normDimensionScore.getDimensionScore(dim);
                if (norm < 10) {
                    this.normDimensionScore.record(dim, norm * 3);
                    this.dimensionScore.record(dim, (int) Math.round(score * 1.8));
                } else if (norm < 20) {
                    this.normDimensionScore.record(dim, norm * 2);
                    this.dimensionScore.record(dim, (int) Math.round(score * 1.4));
                }
            }

            // 正则化
            this.dimensionScore.normalization();
            this.normDimensionScore.normalization();

            // 描述
            PsychologyHelper.fillDimensionScoreDescription(this.service.getTokenizer(), this.dimensionScore);
        } catch (Exception e) {
            Logger.w(this.getClass(), "#make", e);
        }

        // 评估分推理
        List<EvaluationScore> scoreList = this.evaluationReport.getEvaluationScoresByRepresentation(Indicator.values().length);
        this.reportTextList = this.inferScore(scoreList, maxIndicatorTexts);
        if (this.reportTextList.isEmpty()) {
            Logger.w(this.getClass(), "#make - Report text error");
            return this;
        }

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#make - Report list size: " + this.reportTextList.size());
        }

        // 生成概述
        this.summary = this.inferSummary(this.reportTextList);

        // 生成人格描述
        this.inferPersonality(this.evaluationReport.getPersonalityAccelerator());

        // 特征描述
        this.makeDescription();

        // 曼陀罗花
        this.daturaFlower = this.inferDaturaFlower(this.evaluationReport.getPersonalityAccelerator());

        return this;
    }

    private DaturaFlower inferDaturaFlower(PersonalityAccelerator personalityAccelerator) {
        BigFiveFeature feature = personalityAccelerator.getBigFiveFeature();
        List<String> filenames = Resource.getInstance().getDaturaFlowerFiles();
        String color = "A";
        if (feature.getNeuroticism() < 3.0) {
            color = "B";
        }
        else if (feature.getNeuroticism() < 4.0) {
            color = "G";
        }
        else if (feature.getNeuroticism() < 5.0) {
            color = "Y";
        }
        else if (feature.getNeuroticism() < 6.0) {
            color = "R";
        }
        else {
            color = "P";
        }

        List<String> filenameList = new ArrayList<>();
        for (String filename : filenames) {
            if (filename.startsWith(color)) {
                filenameList.add(filename);
            }
        }
        DaturaFlower daturaFlower = new DaturaFlower(
                FileUtils.extractFileName(filenameList.get(Utils.randomInt(0, filenameList.size() - 1))));
        return daturaFlower;
    }

    private List<EvaluationScore> filter(List<EvaluationScore> indicatorList, List<EvaluationScore> sources) {
        List<EvaluationScore> result = new ArrayList<>();
        for (EvaluationScore es : indicatorList) {
            Indicator indicator = es.indicator;
            for (EvaluationScore score : sources) {
                if (score.indicator == indicator) {
                    result.add(score);
                }
            }
        }
        return result;
    }

    private void makeDescription() {
        List<Representation> representations = this.evaluationReport.getRepresentationListByEvaluationScore(100);
        for (Representation representation : representations) {
            String marked = null;
            // 趋势
            if (representation.positiveCorrelation == representation.negativeCorrelation) {
                marked = NormalTrick + representation.knowledgeStrategy.getTerm().word;
            }
            else if (representation.negativeCorrelation > 0 &&
                    representation.positiveCorrelation < representation.negativeCorrelation) {
                marked = LowTrick + representation.knowledgeStrategy.getTerm().word;
            }
            else if (representation.positiveCorrelation >= 3 ||
                    (representation.positiveCorrelation - representation.negativeCorrelation) >= 4) {
                marked = HighTrick + representation.knowledgeStrategy.getTerm().word;
            }
            else {
                marked = NormalTrick + representation.knowledgeStrategy.getTerm().word;
            }

            // 设置短描述
            representation.description = marked;
        }
    }

    /**
     * 推理人格。
     *
     * @param personalityAccelerator
     * @return
     */
    private boolean inferPersonality(PersonalityAccelerator personalityAccelerator) {
        BigFiveFeature feature = personalityAccelerator.getBigFiveFeature();
        String prompt = feature.generateReportPrompt();
        String answer = null;
        if (this.speed) {
            answer = this.infer(prompt);
        }
        if (null == answer) {
            Logger.w(this.getClass(), "#inferPersonality - No answer for \"" + prompt + "\"");

            answer = this.service.syncGenerateText(this.unitName, prompt, new GenerativeOption(),
                    null, null);
        }
        if (null == answer) {
            Logger.w(this.getClass(), "#inferPersonality - report is null: " + prompt);
            return false;
        }
        // 对数据集数据进行推理
        prompt = String.format(PERSONALITY_FORMAT, answer.replaceAll("你", "他"));
        String fixAnswer = this.service.syncGenerateText(ModelConfig.INFINITE_UNIT, prompt, new GenerativeOption(),
                null, null);
        if (null != fixAnswer) {
            answer = fixAnswer;
        }

        // 设置描述
        feature.setDescription(answer);

        // 宜人性
        prompt = feature.generateObligingnessPrompt();
        answer = null;
        if (this.speed) {
            Logger.d(this.getClass(), "#inferPersonality - Obligingness prompt: \"" + prompt + "\"");
            answer = this.infer(prompt);
        }
        if (null == answer) {
            Logger.w(this.getClass(), "#inferPersonality - No answer for \"" + prompt + "\"");
            answer = this.service.syncGenerateText(this.unitName, prompt, new GenerativeOption(),
                    null, null);
        }
        if (null == answer) {
            Logger.w(this.getClass(), "#inferPersonality - Obligingness content error: " + prompt);
        }
        else {
            feature.setObligingnessContent(answer);
        }
        // 宜人性释义
        feature.setObligingnessParaphrase(this.infer(feature.getObligingnessPrompt()));

        // 尽责性
        prompt = feature.generateConscientiousnessPrompt();
        answer = null;
        if (this.speed) {
            Logger.d(this.getClass(), "#inferPersonality - Conscientiousness prompt: \"" + prompt + "\"");
            answer = this.infer(prompt);
        }
        if (null == answer) {
            Logger.w(this.getClass(), "#inferPersonality - No answer for \"" + prompt + "\"");
            answer = this.service.syncGenerateText(this.unitName, prompt, new GenerativeOption(),
                    null, null);
        }
        if (null == answer) {
            Logger.w(this.getClass(), "#inferPersonality - Conscientiousness content error: " + prompt);
        }
        else {
            feature.setConscientiousnessContent(answer);
        }
        // 尽责性释义
        feature.setConscientiousnessParaphrase(this.infer(feature.getConscientiousnessPrompt()));

        // 外向性
        prompt = feature.generateExtraversionPrompt();
        answer = null;
        if (this.speed) {
            Logger.d(this.getClass(), "#inferPersonality - Extraversion prompt: \"" + prompt + "\"");
            answer = this.infer(prompt);
        }
        if (null == answer) {
            Logger.w(this.getClass(), "#inferPersonality - No answer for \"" + prompt + "\"");
            answer = this.service.syncGenerateText(this.unitName, prompt, new GenerativeOption(),
                    null, null);
        }
        if (null == answer) {
            Logger.w(this.getClass(), "#inferPersonality - Extraversion content error: " + prompt);
        }
        else {
            feature.setExtraversionContent(answer);
        }
        // 外向性释义
        feature.setExtraversionParaphrase(this.infer(feature.getExtraversionPrompt()));

        // 进取性
        prompt = feature.generateAchievementPrompt();
        answer = null;
        if (this.speed) {
            Logger.d(this.getClass(), "#inferPersonality - Achievement prompt: \"" + prompt + "\"");
            answer = this.infer(prompt);
        }
        if (null == answer) {
            Logger.w(this.getClass(), "#inferPersonality - No answer for \"" + prompt + "\"");
            answer = this.service.syncGenerateText(this.unitName, prompt, new GenerativeOption(),
                    null, null);
        }
        if (null == answer) {
            Logger.w(this.getClass(), "#inferPersonality - Achievement content error: " + prompt);
        }
        else {
            feature.setAchievementContent(answer);
        }
        // 进取性释义
        feature.setAchievementParaphrase(this.infer(feature.getAchievementPrompt()));

        // 情绪性
        prompt = feature.generateNeuroticismPrompt();
        answer = null;
        if (this.speed) {
            Logger.d(this.getClass(), "#inferPersonality - Neuroticism prompt: \"" + prompt + "\"");
            answer = this.infer(prompt);
        }
        if (null == answer) {
            Logger.w(this.getClass(), "#inferPersonality - No answer for \"" + prompt + "\"");
            answer = this.service.syncGenerateText(this.unitName, prompt, new GenerativeOption(),
                    null, null);
        }
        if (null == answer) {
            Logger.w(this.getClass(), "#inferPersonality - Neuroticism content error: " + prompt);
        }
        else {
            feature.setNeuroticismContent(answer);
        }
        // 情绪性释义
        feature.setNeuroticismParaphrase(this.infer(feature.getNeuroticismPrompt()));

        return true;
    }

    private List<ReportSection> inferScore(List<EvaluationScore> scoreList, int maxIndicatorTexts) {
        List<ReportSection> result = new ArrayList<>();
        for (EvaluationScore es : scoreList) {
            Logger.d(this.getClass(), "#inferScore - score: " + es.indicator.name);

            String prompt = es.generateReportPrompt();
            if (null == prompt) {
                // 不需要进行报告推理，下一个
                continue;
            }

            String report = null;

            if (this.speed) {
                report = this.infer(prompt);
            }
            if (null == report) {
                Logger.w(this.getClass(), "#inferScore - No report for \"" + prompt + "\"");

                report = this.service.syncGenerateText(this.unitName, prompt, new GenerativeOption(),
                        null, null);
            }

            prompt = es.generateSuggestionPrompt();
            if (null == prompt) {
                // 不进行推理，下一个
                continue;
            }

            String suggestion = null;

            if (this.speed) {
                suggestion = this.infer(prompt);
            }
            if (null == suggestion) {
                Logger.w(this.getClass(), "#inferScore - No suggestion for \"" + prompt + "\"");

                suggestion = this.service.syncGenerateText(this.unitName, prompt, new GenerativeOption(),
                        null, null);
            }

            if (null != report && null != suggestion) {
                result.add(new ReportSection(es.indicator, es.generateWord(),
                        report, suggestion));

                if (result.size() >= maxIndicatorTexts) {
                    break;
                }
            }
        }

        return result;
    }

    private String inferSummary(List<ReportSection> list) {
        // 生成概述
        StringBuilder prompt = new StringBuilder("已知信息：\n");
        for (ReportSection rs : list) {
            prompt.append(rs.report).append("\n");
            if (prompt.length() >= ModelConfig.BAIZE_CONTEXT_LIMIT) {
                break;
            }
        }
        prompt.append("\n");
        prompt.append("根据上述已知信息，简洁和专业的来回答用户的问题。问题是：总结一下这个人的心理症状。");
        String summary = this.service.syncGenerateText(this.unitName, prompt.toString(), new GenerativeOption(),
                null, null);

        if (null == summary || summary.contains("我遇到一些问题")) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            summary = this.service.syncGenerateText(this.unitName, prompt.toString(), new GenerativeOption(),
                    null, null);
        }

        return summary;
    }

    public String infer(String query) {
        Dataset dataset = Resource.getInstance().loadDataset();
        if (null == dataset) {
            Logger.w(this.getClass(), "#infer - Read dataset failed");
            return null;
        }

        synchronized (dataset) {
            if (!dataset.hasAnalyzed()) {
                for (String question : dataset.getQuestions()) {
                    TFIDFAnalyzer analyzer = new TFIDFAnalyzer(this.service.getTokenizer());
                    List<Keyword> keywordList = analyzer.analyze(question, 7);
                    if (keywordList.isEmpty()) {
                        continue;
                    }

                    List<String> keywords = new ArrayList<>();
                    for (Keyword keyword : keywordList) {
                        keywords.add(keyword.getWord());
                    }
                    // 填充问题关键词
                    dataset.fillQuestionKeywords(question, keywords.toArray(new String[0]));
                }
            }
        }

        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(this.service.getTokenizer());
        List<Keyword> keywordList = analyzer.analyze(query, 5);
        if (keywordList.isEmpty()) {
            Logger.w(this.getClass(), "#infer - Query keyword is none");
            return null;
        }

        List<String> keywords = new ArrayList<>();
        for (Keyword keyword : keywordList) {
            keywords.add(keyword.getWord());
        }

        return dataset.matchContent(keywords.toArray(new String[0]), 5);
    }

    /**
     * 将内容里的列表数据提取到列表里。
     *
     * @param content
     * @return
     */
    /*private List<String> extractList(String content) {
        List<String> result = new ArrayList<>();
        String[] buf = content.split("\n");
        List<String> lines = new ArrayList<>();
        for (String text : buf) {
            if (text.trim().length() <= 1) {
                continue;
            }

            lines.add(text.trim());

            if (TextUtils.startsWithNumberSign(text.trim())) {
                result.add(text.trim());
            }
        }

        if (result.isEmpty()) {
            Logger.d(this.getClass(), "#extractList - The result is not list, extracts list by symbol");

            int index = 0;
            for (String line : lines) {
                if (line.endsWith("：") || line.endsWith(":")) {
                    continue;
                }

                buf = line.split("。");
                for (String text : buf) {
                    text = text.trim().replaceAll("\n", "");
                    result.add((index + 1) + ". " + text + "。");
                    ++index;
                }
            }
        }

        return result;
    }*/

    /*private String spliceList(List<String> list) {
        StringBuilder buf = new StringBuilder();
        for (String text : list) {
            buf.append(text).append("\n");
        }
        buf.delete(buf.length() - 1, buf.length());
        return buf.toString();
    }*/

    /*private List<String> plainText(List<String> list, boolean sequenceFilter) {
        List<String> result = new ArrayList<>(list.size());
        for (String text : list) {
            String content = text;

            if (sequenceFilter) {
                String[] tmp = text.split("：");
                if (tmp.length == 1) {
                    tmp = text.split(":");
                }
                content = tmp[tmp.length - 1];
                if (TextUtils.startsWithNumberSign(content)) {
                    int index = content.indexOf(".");
                    content = content.substring(index + 1).trim();
                }
            }

            content = content
                    .replaceAll("他们", "你")
                    .replaceAll("他", "你")
                    .replaceAll("这个人", "你")
                    .replaceAll("该人", "你")
                    .replaceAll("该个人", "你")
                    .replaceAll("本人", "你")
                    .replace("人们", "");
            result.add(content);
        }
        return result;
    }*/

    /*private String filterNoise(String text) {
        List<String> content = new ArrayList<>();

        // 按行读取
        List<String> lines = new ArrayList<>();
        String[] tmp = text.split("\n");
        for (String t : tmp) {
            if (t.trim().length() <= 2) {
                continue;
            }
            lines.add(t.trim());
        }
        // 将 Markdown 格式文本转平滑文本
        lines = this.plainText(lines, true);

        for (String line : lines) {
            if (line.contains("根据")) {
                continue;
            }

            String[] sentences = line.split("。");
            for (String sentence : sentences) {
                if (sentence.contains("人工智能") || sentence.contains("AI")) {
                    continue;
                }
                else if (sentence.contains("总的来说") || sentence.contains("总之")) {
                    continue;
                }

                if (sentence.startsWith("但")) {
                    content.add(sentence.replaceFirst("但", ""));
                }
                else {
                    // 进行细分
                    String[] sub = sentence.split("，");
                    StringBuilder ss = new StringBuilder();
                    for (String s : sub) {
                        String rs = s;
                        if (s.contains("然而") || s.contains("但是")) {
                            continue;
                        }
                        else if (s.contains("提供的信息")) {
                            rs = s.replace("提供的信息", "绘画");
                        }

                        ss.append(rs).append("，");
                    }
                    if (ss.length() > 1) {
                        ss.delete(ss.length() - 1, ss.length());
                    }

                    content.add(ss.toString());
                }
            }
        }

        StringBuilder buf = new StringBuilder();
        for (String c : content) {
            buf.append(c).append("。");
        }
        return buf.toString();
    }*/
}
