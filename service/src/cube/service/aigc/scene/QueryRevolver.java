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

import cube.aigc.ModelConfig;
import cube.aigc.psychology.PsychologyReport;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.ThemeTemplate;
import cube.aigc.psychology.composition.EvaluationScore;
import cube.aigc.psychology.composition.ReportRelation;
import cube.common.entity.GenerativeRecord;

import java.util.ArrayList;
import java.util.List;

public class QueryRevolver {

    private String[] keywordThinkingStyle = new String[] {
            "思维方式",
            "思考方式",
            "思维方法",
            "思考方法"
    };

    private String[] keywordCommunicationStyle = new String[] {
            "沟通风格",
            "沟通方式",
            "沟通方法",
            "沟通能力"
    };

    private String[] keywordWorkEnvironment = new String[] {
            "工作环境偏好",
            "工作环境喜好",
            "工作环境倾向"
    };

    private String[] keywordManagementRecommendations = new String[] {
            "管理建议",
            "管理方式",
            "管理方法"
    };

    private String[] keywordQueryPersonality = new String[] {
            "性格", "人格", "胜任"
    };

    public QueryRevolver() {
    }

    public String generatePrompt(ReportRelation relation, PsychologyReport report, String query, boolean extraLong) {
        StringBuilder result = new StringBuilder();

        if (extraLong) {
            ThemeTemplate template = Resource.getInstance().getThemeTemplate(report.getTheme());

            result.append("已知信息：\n当前讨论的受测人的名称是：").append(relation.name).append("，");
            result.append("年龄是：").append(report.getAttribute().age).append("岁，");
            result.append("性别是：").append(report.getAttribute().getGenderText()).append("性。\n");
            result.append("受测人的心理表现有：");
            List<String> symptomContent = this.extractSymptomContent(template,
                    report.getEvaluationReport().getEvaluationScores());
            for (String content : symptomContent) {
                result.append(content);
            }

            if (result.length() < ModelConfig.BAIZE_CONTEXT_LIMIT) {
                result.append("此人的大五人格画像是“").append(report.getEvaluationReport()
                        .getPersonalityAccelerator().getBigFiveFeature().getDisplayName()).append("”。");
                result.append(this.filterPersonalityDescription(report.getEvaluationReport()
                        .getPersonalityAccelerator().getBigFiveFeature().getDescription()));
            }

            result.append("\n根据上述已知信息，简洁和专业的来回答用户的问题。不允许在答案中添加编造成分。问题是：");
            result.append(query);
        }
        else {
            result.append("当前讨论的受测人的名称是：").append(relation.name).append("，");
            result.append("年龄是：").append(report.getAttribute().age).append("岁，");
            result.append("性别是：").append(report.getAttribute().getGenderText()).append("性。\n");
            result.append("其心理症状有：");
            int count = 0;
            for (EvaluationScore es : report.getEvaluationReport().getEvaluationScores()) {
                String word = es.generateWord();
                if (null == word || word.length() == 0) {
                    continue;
                }
                result.append(word).append("、");
                ++count;
                if (count > 7) {
                    break;
                }
            }
            result.delete(result.length() - 1, result.length());
            result.append("。");

            if (null != report.getEvaluationReport().getPersonalityAccelerator()) {
                result.append("此人的大五人格画像是“");
                result.append(report.getEvaluationReport().getPersonalityAccelerator().getBigFiveFeature().getDisplayName());
                result.append("”。");
            }

            // 增加人格描述
            result.append(this.generateFragment(report, query));

            result.append("根据上述信息，作为专业的心理咨询师，请回答：").append(query);
        }

        return result.toString();
    }

    public GenerativeRecord generateSupplement(ReportRelation relation, PsychologyReport report) {
        StringBuilder query = new StringBuilder();
        StringBuilder answer = new StringBuilder();

        query.append("当前我们讨论的个人（受测人）有哪些信息？");
        answer.append("我们现在讨论的受测人的名称是：").append(relation.name).append("，");
        answer.append("年龄是：").append(report.getAttribute().age).append("岁，");
        answer.append("性别是：").append(report.getAttribute().getGenderText()).append("性，");
        answer.append("其心理症状是：");
        int count = 0;
        for (EvaluationScore es : report.getEvaluationReport().getEvaluationScores()) {
            String word = es.generateWord();
            if (null == word || word.length() == 0) {
                continue;
            }
            answer.append(word).append("、");
            ++count;
            if (count > 7) {
                break;
            }
        }
        answer.delete(answer.length() - 1, answer.length());
        answer.append("。");

        if (null != report.getEvaluationReport().getPersonalityAccelerator()) {
            answer.append("此人的大五人格画像是“");
            answer.append(report.getEvaluationReport().getPersonalityAccelerator().getBigFiveFeature().getDisplayName());
            answer.append("”。");
        }

        GenerativeRecord result = new GenerativeRecord(ModelConfig.BAIZE_UNIT, query.toString(), answer.toString());
        return result;
    }

    private List<String> extractSymptomContent(ThemeTemplate theme, List<EvaluationScore> list) {
        List<String> result = new ArrayList<>();

        for (EvaluationScore es : list) {
            String word = es.generateWord();
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

    private String generateFragment(PsychologyReport report, String query) {
        StringBuilder result = new StringBuilder();

        if (null != report.getEvaluationReport().getPersonalityAccelerator()) {
            result.append("回答问题时可使用以下知识：");

            for (String word : this.keywordThinkingStyle) {
                if (query.contains(word)) {
                    result.append("“");
                    result.append(report.getEvaluationReport().getPersonalityAccelerator()
                            .getBigFiveFeature().getDisplayName());
                    result.append("的思维方式”，");
                    break;
                }
            }

            for (String word : this.keywordCommunicationStyle) {
                if (query.contains(word)) {
                    result.append("“");
                    result.append(report.getEvaluationReport().getPersonalityAccelerator()
                            .getBigFiveFeature().getDisplayName());
                    result.append("的沟通风格”，");
                    break;
                }
            }

            for (String word : this.keywordWorkEnvironment) {
                if (query.contains(word)) {
                    result.append("“");
                    result.append(report.getEvaluationReport().getPersonalityAccelerator()
                            .getBigFiveFeature().getDisplayName());
                    result.append("的工作环境偏好”，");
                    break;
                }
            }

            for (String word : this.keywordManagementRecommendations) {
                if (query.contains(word)) {
                    result.append("“");
                    result.append(report.getEvaluationReport().getPersonalityAccelerator()
                            .getBigFiveFeature().getDisplayName());
                    result.append("的管理建议”，");
                    break;
                }
            }

            if (result.length() <= 17) {
                result.delete(0, result.length());
            }
            else {
                result.delete(result.length() - 1, result.length());
                result.append("。");
            }
        }

        return result.toString();
    }

    private String filterPersonalityDescription(String desc) {
        return desc.replaceAll("你", "他");
    }
}
