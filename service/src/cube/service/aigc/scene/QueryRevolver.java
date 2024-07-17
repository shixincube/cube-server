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

import cube.aigc.psychology.PsychologyReport;
import cube.aigc.psychology.composition.EvaluationScore;

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

    public QueryRevolver() {
    }

    public String generatePrompt(PsychologyReport report, String query) {
        StringBuilder result = new StringBuilder();

        result.append("当前讨论对象（个体）的年龄是");
        result.append(report.getAttribute().age).append("岁");
        result.append("，性别是").append(report.getAttribute().getGenderText()).append("性");
        result.append("，其心理症状是：");
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

        result.append(this.generateFragment(report, query));

        result.append("请回答：").append(query);

        return result.toString();
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
}
