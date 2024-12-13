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

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.*;
import cube.aigc.psychology.composition.*;
import cube.common.entity.GenerativeRecord;
import cube.service.tokenizer.Tokenizer;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;

import java.util.ArrayList;
import java.util.List;

public class QueryRevolver {

//    private String[] keywordThinkingStyle = new String[] {
//            "思维方式",
//            "思考方式",
//            "思维方法",
//            "思考方法",
//            "思维",
//            "思考"
//    };

//    private String[] keywordCommunicationStyle = new String[] {
//            "沟通风格",
//            "沟通方式",
//            "沟通方法",
//            "沟通能力",
//            "沟通",
//            "交流"
//    };

    private String[] keywordWorkEnvironment = new String[] {
            "工作环境偏好",
            "工作环境喜好",
            "工作环境倾向",
            "工作环境"
    };

    private String[] keywordManagementRecommendation = new String[] {
            "管理建议",
            "管理方式",
            "管理方法",
            "管理"
    };

    private String[] keywordSuggestion = new String[] {
            "建议",
            "改善",
            "缓解",
            "解决"
    };

//    private String[] keywordQueryPersonality = new String[] {
//            "性格", "人格", "胜任"
//    };

    private String[] paintingDesc = new String[] {
            "画", "画面", "图画", "图像", "照片", "绘画", "看"
    };

    private Tokenizer tokenizer;

    private PsychologyStorage storage;

    public QueryRevolver(Tokenizer tokenizer, PsychologyStorage storage) {
        this.tokenizer = tokenizer;
        this.storage = storage;
    }

    public String generatePrompt(ReportRelation relation, Report report, String query) {
        StringBuilder result = new StringBuilder();

        result.append("已知信息：\n当前受测人的名称是：").append(this.fixName(
                relation.name,
                report.getAttribute().getGenderText(),
                report.getAttribute().age)).append("，");
        result.append("年龄是：").append(report.getAttribute().age).append("岁，");
        result.append("性别是：").append(report.getAttribute().getGenderText()).append("性。\n");

        if (report instanceof PaintingReport) {
            result.append("受测人的情况如下：\n");

            PaintingReport paintingReport = (PaintingReport) report;

            ThemeTemplate template = Resource.getInstance().getThemeTemplate(paintingReport.getTheme());

            List<String> symptomContent = this.extractSymptomContent(template,
                    paintingReport.getEvaluationReport().getEvaluationScores(), report.getAttribute());
            for (String content : symptomContent) {
                result.append(content);
                if (result.length() > ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                    break;
                }
            }
            result.append("\n");

            result.append("\n");
            result.append(this.tryGeneratePaintingFeature(paintingReport, query));

            if (result.length() < ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                result.append("\n受测人的大五人格画像是").append(paintingReport.getEvaluationReport()
                        .getPersonalityAccelerator().getBigFivePersonality().getDisplayName()).append("。\n");
                // 性格特点
                result.append(paintingReport.getEvaluationReport()
                        .getPersonalityAccelerator().getBigFivePersonality().getDisplayName()).append("的性格特点：");
                result.append(this.filterPersonalityDescription(paintingReport.getEvaluationReport()
                        .getPersonalityAccelerator().getBigFivePersonality().getDescription()));
            }

            if (result.length() < ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                result.append("\n");
                result.append(this.generateFragment(report, query));
            }
        }
        else if (report instanceof ScaleReport) {
            ScaleReport scaleReport = (ScaleReport) report;
            if (null != scaleReport.getScale()) {
                result.append(scaleReport.getScale().displayName);
                result.append("量表的测验结果是：\n");
            }
            else {
                result.append("受测人的心理表现有：\n");
            }

            for (ScaleFactor factor : scaleReport.getFactors()) {
                result.append("\n* ").append(factor.description);
                if (result.length() > ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                    break;
                }
            }

            result.append("\n对于上述心理表现给出以下建议：\n");
            for (ScaleFactor factor : scaleReport.getFactors()) {
                result.append("\n* ").append(factor.suggestion);
                if (result.length() > ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                    break;
                }
            }
            result.append("\n");
        }

        result.append("\n根据上述已知信息，简洁和专业的来回答用户的问题。");

        for (String word : this.keywordSuggestion) {
            if (query.contains(word)) {
                result.append("答案里不要出现药品信息。");
                break;
            }
        }

        result.append("问题是：");
        result.append(query);

        return this.filterSubjectNoun(result.toString());
    }

    public String generatePrompt(List<ReportRelation> relations, List<Report> reports, String query) {
        StringBuilder result = new StringBuilder();

        result.append("已知信息：\n\n");
        result.append("现有").append(relations.size()).append("个人的相关信息如下：\n\n");

        for (int i = 0; i < relations.size(); ++i) {
            ReportRelation relation = relations.get(i);
            Report report = reports.get(i);
            String name = relation.name;
//            String name = this.fixName(relation.name, report.getAttribute().getGenderText(), report.getAttribute().age);
            result.append("## ").append(name).append("\n\n");
            result.append(name).append("的年龄是：").append(report.getAttribute().age).append("岁，");
            result.append("性别是：").append(report.getAttribute().getGenderText()).append("性。\n");

            if (report instanceof PaintingReport) {
                result.append(name).append("的情况如下：\n");

                PaintingReport paintingReport = (PaintingReport) report;

                ThemeTemplate template = Resource.getInstance().getThemeTemplate(paintingReport.getTheme());

                List<String> symptomContent = this.extractSymptomContent(template,
                        paintingReport.getEvaluationReport().getEvaluationScores(), paintingReport.getAttribute());
                for (String content : symptomContent) {
                    result.append(content);
                    if (result.length() > ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                        break;
                    }
                }
                result.append("\n");

                if (result.length() < ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                    result.append("\n");
                    result.append(name).append("的大五人格画像是").append(paintingReport.getEvaluationReport()
                            .getPersonalityAccelerator().getBigFivePersonality().getDisplayName()).append("。\n");
                    // 性格特点
                    result.append(paintingReport.getEvaluationReport()
                            .getPersonalityAccelerator().getBigFivePersonality().getDisplayName()).append("的性格特点：");
                    result.append(this.filterPersonalityDescription(paintingReport.getEvaluationReport()
                            .getPersonalityAccelerator().getBigFivePersonality().getDescription()));
                }

                if (result.length() < ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                    result.append("\n");
                    result.append(this.generateFragment(report, query));
                }
            }
            else if (report instanceof ScaleReport) {
                ScaleReport scaleReport = (ScaleReport) report;
                if (null != scaleReport.getScale()) {
                    result.append(scaleReport.getScale().displayName);
                    result.append("量表的测验结果是：\n");
                }
                else {
                    result.append(name).append("的心理表现有：\n");
                }

                for (ScaleFactor factor : scaleReport.getFactors()) {
                    result.append("\n* ").append(factor.description);
                    if (result.length() > ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                        break;
                    }
                }

                result.append("\n对于上述心理表现给出以下建议：\n");
                for (ScaleFactor factor : scaleReport.getFactors()) {
                    result.append("\n* ").append(factor.suggestion);
                    if (result.length() > ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                        break;
                    }
                }
                result.append("\n");
            }

            if (result.substring(result.length() - 1).equals("\n")) {
                result.append("\n");
            }
            else {
                result.append("\n\n");
            }
        }

        result.append("\n根据上述已知信息，简洁和专业的来回答用户的问题。");

        for (String word : this.keywordSuggestion) {
            if (query.contains(word)) {
                result.append("答案里不要出现药品信息。");
                break;
            }
        }

        result.append("问题是：");
        result.append(query);

        return this.filterSubjectNoun(result.toString());
    }

    public GenerativeRecord generateSupplement(ReportRelation relation, Report report, String currentQuery) {
        StringBuilder query = new StringBuilder();
        StringBuilder answer = new StringBuilder();

        query.append("当前讨论的受测人有哪些信息？");

        answer.append("当前讨论的受测人的名称是：").append(this.fixName(relation.name,
                        report.getAttribute().getGenderText(),
                        report.getAttribute().age)).append("，");
        answer.append("年龄是：").append(report.getAttribute().age).append("岁，");
        answer.append("性别是：").append(report.getAttribute().getGenderText()).append("性。\n");

        if (report instanceof PaintingReport) {
            answer.append("受测人的情况如下：");

            PaintingReport paintingReport = (PaintingReport) report;

            ThemeTemplate template = Resource.getInstance().getThemeTemplate(paintingReport.getTheme());

            List<String> symptomContent = this.extractSymptomContent(template,
                    paintingReport.getEvaluationReport().getEvaluationScores(), paintingReport.getAttribute());
            for (String content : symptomContent) {
                answer.append(content);
                if (answer.length() > ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                    break;
                }
            }

            if (answer.length() < ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                answer.append("\n受测人的大五人格画像是").append(paintingReport.getEvaluationReport()
                        .getPersonalityAccelerator().getBigFivePersonality().getDisplayName()).append("。");
                answer.append(this.filterPersonalityDescription(paintingReport.getEvaluationReport()
                        .getPersonalityAccelerator().getBigFivePersonality().getDescription()));
            }

            if (answer.length() < ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                answer.append("\n");
                answer.append(this.generateFragment(report, currentQuery));
            }
        }
        else if (report instanceof ScaleReport) {
            ScaleReport scaleReport = (ScaleReport) report;
            if (null != scaleReport.getScale()) {
                answer.append(scaleReport.getScale().displayName);
                answer.append("量表的测验结果是：\n");
            }
            else {
                answer.append("受测人的心理表现有：\n");
            }

            for (ScaleFactor factor : scaleReport.getFactors()) {
                answer.append("\n* ").append(factor.description);
                if (answer.length() > ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                    break;
                }
            }

            answer.append("\n对于上述心理表现给出以下建议：\n");
            for (ScaleFactor factor : scaleReport.getFactors()) {
                answer.append("\n* ").append(factor.suggestion);
                if (answer.length() > ModelConfig.EXTRA_LONG_CONTEXT_LIMIT) {
                    break;
                }
            }
        }

        GenerativeRecord result = new GenerativeRecord(ModelConfig.BAIZE_UNIT, query.toString(), answer.toString());
        return result;
    }

    public String generatePrompt(CustomRelation relation, String query) {
        String knowledge = this.generateKnowledge(query);
        if (null == knowledge || knowledge.length() < 2) {
            return query;
        }

        StringBuilder result = new StringBuilder();
        result.append("已知信息：\n");
        result.append(knowledge).append("\n\n");
        result.append("根据以上信息，专业地回答问题。问题是：");
        result.append(query);

        return result.toString();
    }

    private String fixName(String name, String gender, int age) {
        return name;
//        if (age <= 22) {
//            return name.charAt(0) + "同学";
//        }
//        else {
//            return name.charAt(0) + (gender.contains("男") ? "先生" : "女士");
//        }
    }

    private List<String> extractSymptomContent(ThemeTemplate theme, List<EvaluationScore> list, Attribute attribute) {
        List<String> result = new ArrayList<>();

        for (EvaluationScore es : list) {
            String word = es.generateWord(attribute);
            if (null == word) {
                continue;
            }

            ThemeTemplate.SymptomContent symptomContent = theme.findSymptomContent(word);
            if (null == symptomContent) {
                continue;
            }

            result.add(symptomContent.content + "\n");
        }

        return result;
    }

    private String generateFragment(Report report, String query) {
        StringBuilder result = new StringBuilder();

        if (report instanceof PaintingReport) {
            PaintingReport paintingReport = (PaintingReport) report;

            if (null != paintingReport.getEvaluationReport().getPersonalityAccelerator()) {
                result.append("\n");
                String question = null;
                String answer = null;
                TFIDFAnalyzer analyzer = null;
                List<String> keywords = null;

//                for (String word : this.keywordThinkingStyle) {
//                    if (query.contains(word))
                question = paintingReport.getEvaluationReport().getPersonalityAccelerator()
                        .getBigFivePersonality().getDisplayName() + "的思维方式";
                analyzer = new TFIDFAnalyzer(this.tokenizer);
                keywords = analyzer.analyzeOnlyWords(question, 10);

                answer = Resource.getInstance().loadDataset().matchContent(keywords.toArray(new String[0]), 5);
                if (null != answer) {
                    result.append(question).append("：");
                    result.append(answer).append("\n\n");
                }
//                        break;
//                    }
//                }

//                for (String word : this.keywordCommunicationStyle) {
//                    if (query.contains(word)) {
                question = paintingReport.getEvaluationReport().getPersonalityAccelerator()
                        .getBigFivePersonality().getDisplayName() + "的沟通风格";
                analyzer = new TFIDFAnalyzer(this.tokenizer);
                keywords = analyzer.analyzeOnlyWords(question, 10);

                answer = Resource.getInstance().loadDataset().matchContent(keywords.toArray(new String[0]), 5);
                if (null != answer) {
                    result.append(question).append("：");
                    result.append(answer).append("\n\n");
                }
//                        break;
//                    }
//                }

                for (String word : this.keywordWorkEnvironment) {
                    if (query.contains(word)) {
//                        result.append("“");
//                        result.append(paintingReport.getEvaluationReport().getPersonalityAccelerator()
//                                .getBigFiveFeature().getDisplayName());
//                        result.append("的工作环境偏好”，");

                        question = paintingReport.getEvaluationReport().getPersonalityAccelerator()
                                .getBigFivePersonality().getDisplayName() + "的工作环境偏好";
                        analyzer = new TFIDFAnalyzer(this.tokenizer);
                        keywords = analyzer.analyzeOnlyWords(question, 10);

                        answer = Resource.getInstance().loadDataset().matchContent(keywords.toArray(new String[0]), 5);
                        if (null != answer) {
                            result.append(question).append("：");
                            result.append(answer).append("\n\n");
                        }

                        break;
                    }
                }

                for (String word : this.keywordManagementRecommendation) {
                    if (query.contains(word)) {
//                        result.append("“");
//                        result.append(paintingReport.getEvaluationReport().getPersonalityAccelerator()
//                                .getBigFiveFeature().getDisplayName());
//                        result.append("的管理建议”，");

                        question = paintingReport.getEvaluationReport().getPersonalityAccelerator()
                                .getBigFivePersonality().getDisplayName() + "的管理建议";
                        analyzer = new TFIDFAnalyzer(this.tokenizer);
                        keywords = analyzer.analyzeOnlyWords(question, 10);

                        answer = Resource.getInstance().loadDataset().matchContent(keywords.toArray(new String[0]), 5);
                        if (null != answer) {
                            result.append(question).append("：");
                            result.append(answer).append("\n\n");
                        }

                        break;
                    }
                }

                if (result.length() <= 17) {
                    result.delete(0, result.length());
                }
//                else {
//                    result.delete(result.length() - 1, result.length());
//                    result.append("。");
//                }
            }
        }

        return result.toString();
    }

    private String generateKnowledge(String query) {
        String fixQuery = query.replaceAll("你", "爱心理");

        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(this.tokenizer);
        List<String> keywords = analyzer.analyzeOnlyWords(fixQuery, 7);

        StringBuilder buf = new StringBuilder();

        Dataset dataset = Resource.getInstance().loadDataset();
        List<String> list = dataset.searchContent(keywords.toArray(new String[0]), 2);
        for (String content : list) {
            buf.append(content).append("\n\n");
        }

        return buf.toString();
    }

    private String tryGeneratePaintingFeature(PaintingReport paintingReport, String query) {
        StringBuilder buf = new StringBuilder();

        boolean hit = false;
        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(this.tokenizer);
        List<String> keywords = analyzer.analyzeOnlyWords(query, 10);
        for (String keyword : keywords) {
            for (String word : this.paintingDesc) {
                if (keyword.contains(word)) {
                    hit = true;
                    break;
                }
            }
            if (hit) {
                break;
            }
        }

        if (hit) {
            PaintingFeatureSet featureSet = this.storage.readPaintingFeatureSet(paintingReport.sn);
            if (null != featureSet) {
                buf.append(featureSet.makePrompt(false));
            }
            else {
                Logger.w(this.getClass(), "#tryGeneratePaintingFeature - Can NOT find painting feature set data: " +
                        paintingReport.sn);
            }
        }

        return buf.toString();
    }

    private String filterPersonalityDescription(String desc) {
        return desc.replaceAll("你", "他");
    }

    private String filterSubjectNoun(String content) {
        return content.replaceAll("受测人", "他");
    }
}
