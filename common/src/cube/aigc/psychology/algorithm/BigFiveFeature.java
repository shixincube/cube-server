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

package cube.aigc.psychology.algorithm;

import cube.aigc.psychology.composition.BigFivePersonality;
import cube.common.JSONable;
import cube.util.JSONUtils;
import cube.vision.Point;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 大五人格特征。
 */
public class BigFiveFeature implements JSONable {

    /**
     * 通才。
     */
    public final static BigFiveFeature Generalist = new BigFiveFeature("Generalist", "通才",
            new TemplateValue(7.5, 7.5,
            7.5, 7.5, 5.5));

    /**
     * 倡导者。
     */
    public final static BigFiveFeature Advocate = new BigFiveFeature("Advocate", "倡导者",
            new TemplateValue(7.5, 3.5,
            7.5, 7.5, 5.5));

    /**
     * 创业者。
     */
    public final static BigFiveFeature Entrepreneur = new BigFiveFeature("Entrepreneur", "创业者",
            new TemplateValue(3.5, 7.5,
            7.5, 7.5, 5.5));

    /**
     * 传统者。
     */
    public final static BigFiveFeature Traditionalist = new BigFiveFeature("Traditionalist", "传统者",
            new TemplateValue(7.5, 7.5,
            3.5, 7.5, 5.5));

    /**
     * 开发者。
     */
    public final static BigFiveFeature Developer = new BigFiveFeature("Developer", "开发者",
            new TemplateValue(7.5, 7.5,
            7.5, 3.5, 5.5));

    /**
     * 推广者。
     */
    public final static BigFiveFeature Promoter = new BigFiveFeature("Promoter", "推广者",
            new TemplateValue(3.5, 3.5,
            7.5, 7.5, 5.5));

    /**
     * 实效者。
     */
    public final static BigFiveFeature Realist = new BigFiveFeature("Realist", "实效者",
            new TemplateValue(3.5, 7.5,
            3.5, 7.5, 5.5));

    /**
     * 理想者。
     */
    public final static BigFiveFeature Idealist = new BigFiveFeature("Idealist", "理想者",
            new TemplateValue(7.5, 3.5,
            3.5, 7.5, 5.5));

    /**
     * 辅导教练。
     */
    public final static BigFiveFeature Instructor = new BigFiveFeature("Instructor", "辅导教练",
            new TemplateValue(7.5, 7.5,
            3.5, 3.5, 5.5));

    /**
     * 演示者。
     */
    public final static BigFiveFeature Demonstrator = new BigFiveFeature("Demonstrator", "演示者",
            new TemplateValue(3.5, 7.5,
            7.5, 3.5, 5.5));

    /**
     * 引导者。
     */
    public final static BigFiveFeature Guide = new BigFiveFeature("Guide", "引导者",
            new TemplateValue(7.5, 3.5,
            7.5, 3.5, 5.5));

    /**
     * 建筑师。
     */
    public final static BigFiveFeature Architect = new BigFiveFeature("Architect", "建筑师",
            new TemplateValue(3.5, 3.5,
            3.5, 7.5, 5.5));

    /**
     * 探索者。
     */
    public final static BigFiveFeature Explorer = new BigFiveFeature("Explorer", "探索者",
            new TemplateValue(3.5, 3.5,
            7.5, 3.5, 5.5));

    /**
     * 支持者。
     */
    public final static BigFiveFeature Supporter = new BigFiveFeature("Supporter", "支持者",
            new TemplateValue(7.5, 3.5,
            3.5, 3.5, 5.5));

    /**
     * 控制者。
     */
    public final static BigFiveFeature Controller = new BigFiveFeature("Controller", "控制者",
            new TemplateValue(3.5, 7.5,
            3.5, 3.5, 5.5));

    /**
     * 专家。
     */
    public final static BigFiveFeature Expert = new BigFiveFeature("Expert", "专家",
            new TemplateValue(3.5, 3.5,
            3.5, 3.5, 5.5));

    /**
     * 适应者。
     */
    public final static BigFiveFeature Adapter = new BigFiveFeature("Adapter", "适应者",
            new TemplateValue(5.5, 5.5,
            5.5, 5.5, 5.5));


    public final static double HighScore = 7.0;

    public final static double LowScore = 4.0;


    /**
     * 宜人性。
     */
    private double obligingness;

    private String obligingnessParaphrase = "";

    private String obligingnessContent = "";

    private ScoreAnnotation obligingnessAnnotation = new ScoreAnnotation(
            new String[] {
                    "有同情心、关心别人。", "愿意支持别人。", "和善、对人真诚。", "开明、能理解信任别人。"
            }, new String[] {
                    "过于天真、易受骗。", "思维简单、易感。", "心太软、过于温和。"
            }, new String[] {
                    "讲求实际。", "机敏。", "精明。", "务实、在商言商。"
            }, new String[] {
                    "以自我为中心。", "过于激进、愤世嫉俗。", "有攻击性、负面看人。", "缺乏同情心。"
            });

    /**
     * 尽责性。
     */
    private double conscientiousness;

    private String conscientiousnessParaphrase = "";

    private String conscientiousnessContent = "";

    private ScoreAnnotation conscientiousnessAnnotation = new ScoreAnnotation(
            new String[] {
                    "有计划、未雨绸缪。", "忠诚可靠、有职业道德。", "尽职尽责、实干。", "尊重维护秩序、结构。"
            }, new String[] {
                    "独裁主义。", "过于内向。", "狭隘、不够宽容。", "拒绝变革。"
            }, new String[] {
                    "有创造性。", "放荡不羁、不受局限。", "思想自由、思维开阔。", "临场发挥较好。"
            }, new String[] {
                    "责任心不强、不谨慎。", "不可靠。", "不集中、专注性不够。", "无计划性。"
            });

    /**
     * 外向性。
     */
    private double extraversion;

    private String extraversionParaphrase = "";

    private String extraversionContent = "";

    private ScoreAnnotation extraversionAnnotation = new ScoreAnnotation(
            new String[] {
                    "积极的、精力充沛的。", "热情外向的。", "好交际的。", "乐观的、友善的。"
            }, new String[] {
                    "易于分心、不够专注。", "过于爱展现自己。", "容易打断他人、不顾他人感受。", "容易超出能力许下承诺。"
            }, new String[] {
                    "安静。", "谨慎。", "含蓄、内敛。"
            }, new String[] {
                    "冷淡、漠不关心。", "狭隘。", "不爱交际、沉默寡言。"
            });

    /**
     * 进取性。
     */
    private double achievement;

    private String achievementParaphrase = "";

    private String achievementContent = "";

    private ScoreAnnotation achievementAnnotation = new ScoreAnnotation(
            new String[] {
                    "有支配力、有决心。", "坚定的、完全投入的。", "有推动力的。", "以目标为导向的。"
            }, new String[] {
                    "固执己见。", "好辩论。", "压制他人。"
            }, new String[] {
                    "有伸缩性、灵活。", "适应性强。", "愿意聆听别人意见、与人合作。"
            }, new String[] {
                    "太容易被说服。", "依赖性很重。", "优柔寡断、犹豫不决。"
            });

    /**
     * 情绪性。
     */
    private double neuroticism;

    private String neuroticismParaphrase = "";

    private String neuroticismContent = "";

    private ScoreAnnotation neuroticismAnnotation = new ScoreAnnotation(
            new String[] {
                    "易动感情、易兴奋。", "反应快。", "感染力和带动性，能鼓舞人心。", "有洞察力，会不断提问和澄清。", "乐于学习和发展。"
            }, new String[] {
                    "反复无常、难以预测。", "感情用事、心烦意乱。", "忧虑、缺乏信心。", "易变、不一致。",
                    "直到某项任务结束，否则不愿意停止。", "需要反复确认。", "易受工作环境和他人的影响。"
            }, new String[] {
                    "稳定、不慌乱。", "有一贯性、可预测。", "自信、冷静，逆境中顺其自然。", "善于应对危机。", "积极向上，能够自我激励。"
            }, new String[] {
                    "自满、高估自己的能力。", "低估风险。", "拒绝成长，认为智力维度比个人后天发展更重要。", "不易动情。",
                    "缺乏激情、沉闷。", "感觉不到他人的焦虑。"
            });

    private String name;

    private String displayName;

    private String description = "";

    private TemplateValue templateValue;

    public BigFiveFeature(String name, String displayName, TemplateValue templateValue) {
        this.name = name;
        this.displayName = displayName;
        this.templateValue = templateValue;
    }

    public BigFiveFeature(double obligingness, double conscientiousness,
                          double extraversion, double achievement, double neuroticism) {
        this.obligingness = obligingness;
        this.conscientiousness = conscientiousness;
        this.extraversion = extraversion;
        this.achievement = achievement;
        this.neuroticism = neuroticism;
        this.build();
    }

    public BigFiveFeature(JSONObject json) {
        this.name = json.getString("name");
        this.displayName = json.getString("displayName");
        this.description = json.getString("description");
        JSONArray scores = json.getJSONArray("scores");
        for (int i = 0; i < scores.length(); ++i) {
            JSONObject factor = scores.getJSONObject(i);
            String code = factor.getString("code");
            double score = factor.getDouble("score");
            String paraphrase = factor.has("paraphrase") ? factor.getString("paraphrase") : "";
            String content = factor.has("content") ? factor.getString("content") : "";
            switch (BigFivePersonality.parse(code)) {
                case Obligingness:
                    this.obligingness = score;
                    this.obligingnessParaphrase = paraphrase;
                    this.obligingnessContent = content;
                    break;
                case Conscientiousness:
                    this.conscientiousness = score;
                    this.conscientiousnessParaphrase = paraphrase;
                    this.conscientiousnessContent = content;
                    break;
                case Extraversion:
                    this.extraversion = score;
                    this.extraversionParaphrase = paraphrase;
                    this.extraversionContent = content;
                    break;
                case Achievement:
                    this.achievement = score;
                    this.achievementParaphrase = paraphrase;
                    this.achievementContent = content;
                    break;
                case Neuroticism:
                    this.neuroticism = score;
                    this.neuroticismParaphrase = paraphrase;
                    this.neuroticismContent = content;
                    break;
                default:
                    break;
            }
        }
    }

    private void build() {
        BigFiveFeature[] features = new BigFiveFeature[] {
                Generalist,
                Advocate,
                Entrepreneur,
                Traditionalist,
                Developer,
                Promoter,
                Realist,
                Idealist,
                Instructor,
                Demonstrator,
                Guide,
                Architect,
                Explorer,
                Supporter,
                Controller,
                Expert,
                Adapter
        };

        int index = 0;

        ApproximateValue[] approximateValues = new ApproximateValue[features.length];
        for (BigFiveFeature feature : features) {
            ApproximateValue value = feature.calcApproximate(this.obligingness, this.conscientiousness,
                    this.extraversion, this.achievement);
            approximateValues[index++] = value;
        }

        int hit4Idx = -1;
        List<Integer> hit3IdxList = new ArrayList<>();
        List<Integer> hit2IdxList = new ArrayList<>();
        List<Integer> hit1IdxList = new ArrayList<>();

        int hitIndex = -1;

        index = 0;
        for (ApproximateValue value : approximateValues) {
            int num = value.getNumOfNearValues();
            if (num == 4) {
                hit4Idx = index;
            } else if (num == 3) {
                hit3IdxList.add(index);
            } else if (num == 2) {
                hit2IdxList.add(index);
            } else if (num == 1) {
                hit1IdxList.add(index);
            }
            ++index;
        }

        BigFiveFeature feature = null;

        if (hit4Idx > -1) {
            feature = features[hit4Idx];
        }
        else if (!hit3IdxList.isEmpty()) {
            hitIndex = this.extractIndex(approximateValues, hit3IdxList);
        }
        else if (!hit2IdxList.isEmpty()) {
            hitIndex = this.extractIndexByTrend(approximateValues, hit2IdxList);
        }
        else if (!hit1IdxList.isEmpty()) {
            hitIndex = this.extractIndexByTrend(approximateValues, hit1IdxList);
        }
        else {
            hitIndex = this.extractIndexByTrendAcc(approximateValues);
        }

        if (hitIndex > -1) {
            feature = features[hitIndex];
        }

        if (null == feature) {
            feature = this.guess();
        }

        // 通才、适应者和专家额外处理
        if (feature == BigFiveFeature.Generalist || feature == BigFiveFeature.Adapter || feature == BigFiveFeature.Expert) {
            if (this.obligingness >= 7.0 && this.conscientiousness >= 7.0 &&
                    this.extraversion >= 7.0 && this.achievement >= 7.0) {
                feature = BigFiveFeature.Generalist;
            }
            else if (this.obligingness >= 4.5 && this.conscientiousness >= 4.5 &&
                    this.extraversion >= 4.5 && this.achievement >= 4.5) {
                feature = BigFiveFeature.Adapter;
            }
            else {
                feature = BigFiveFeature.Expert;
            }
        }

        this.name = feature.name;
        this.displayName = feature.displayName;
    }

    private int extractIndex(ApproximateValue[] approximateValues, List<Integer> indexList) {
        double minValue = Double.MAX_VALUE - 1;
        int candidate = -1;
        for (Integer index : indexList) {
            ApproximateValue value = approximateValues[index];
            if (!value.nearObligingness && Math.abs(value.deltaObligingness) < minValue) {
                minValue = Math.abs(value.deltaObligingness);
                candidate = index;
            }
            if (!value.nearConscientiousness && Math.abs(value.deltaConscientiousness) < minValue) {
                minValue = Math.abs(value.deltaConscientiousness);
                candidate = index;
            }
            if (!value.nearExtraversion && Math.abs(value.deltaExtraversion) < minValue) {
                minValue = Math.abs(value.deltaExtraversion);
                candidate = index;
            }
            if (!value.nearAchievement && Math.abs(value.deltaAchievement) < minValue) {
                minValue = Math.abs(value.deltaAchievement);
                candidate = index;
            }
        }
        return candidate;
    }

    private int extractIndexByTrend(ApproximateValue[] approximateValues, List<Integer> indexList) {
        List<Integer> tendencyCount = new ArrayList<>();
        for (Integer index : indexList) {
            ApproximateValue value = approximateValues[index];
            int count = 0;
            if (value.tendencyObligingness != 0) {
                count += 1;
            }
            if (value.tendencyConscientiousness != 0) {
                count += 1;
            }
            if (value.tendencyExtraversion != 0) {
                count += 1;
            }
            if (value.tendencyAchievement != 0) {
                count += 1;
            }
            tendencyCount.add(count);
        }

        int index = 0;
        int max = -1;
        for (int i = 0; i < tendencyCount.size(); ++i) {
            int count = tendencyCount.get(i);
            if (count > max) {
                max = count;
                index = i;
            }
        }

        return indexList.get(index);
    }

    private int extractIndexByTrendAcc(ApproximateValue[] approximateValues) {
        List<Integer> indexList = new ArrayList<>();
        for (int i = 0; i < approximateValues.length; ++i) {
            ApproximateValue value = approximateValues[i];
            int numTendency = value.getNumOfTendency();
            if (numTendency == 4) {
                return i;
            }

            indexList.add(i);
        }

        List<Double> accValueList = new ArrayList<>();
        double accValue = 0;
        for (Integer index : indexList) {
            ApproximateValue value = approximateValues[index];
            accValue = 0;
            if (0 == value.tendencyObligingness) {
                accValue += Math.abs(value.deltaObligingness);
            }
            if (0 == value.tendencyConscientiousness) {
                accValue += Math.abs(value.deltaConscientiousness);
            }
            if (0 == value.tendencyExtraversion) {
                accValue += Math.abs(value.deltaExtraversion);
            }
            if (0 == value.tendencyAchievement) {
                accValue += Math.abs(value.deltaAchievement);
            }
            accValueList.add(accValue);
        }

        int index = -1;
        double minValue = Double.MAX_VALUE - 1;
        for (int i = 0; i < accValueList.size(); ++i) {
            double acc = accValueList.get(i);
            if (acc < minValue) {
                minValue = acc;
                index = i;
            }
        }

        return indexList.get(index);
    }


    private BigFiveFeature guess() {
        BigFiveFeature[] features = new BigFiveFeature[] {
                Generalist,
                Advocate,
                Entrepreneur,
                Traditionalist,
                Developer,
                Promoter,
                Realist,
                Idealist,
                Instructor,
                Demonstrator,
                Guide,
                Architect,
                Explorer,
                Supporter,
                Controller,
                Expert,
                Adapter
        };

        Point p1 = new Point(10 - this.obligingness, 10);
        Point p2 = new Point(10, 10 + this.conscientiousness);
        Point p3 = new Point(10 + this.extraversion, 10);
        Point p4 = new Point(10, 10 - this.achievement);
        // 计算质心
        Point centroid = Point.getCentroid(new Point[] {
                p1, p2, p3, p4
        });

        double distance = 100;
        BigFiveFeature feature = null;
        for (BigFiveFeature bff : features) {
            double d = bff.templateValue.centroid.distance(centroid);
            if (d < distance) {
                distance = d;
                feature = bff;
            }
        }

        // 通才、适应者和专家的画像矩形的质心一致，额外处理
        if (feature == BigFiveFeature.Generalist || feature == BigFiveFeature.Adapter || feature == BigFiveFeature.Expert) {
            if (this.obligingness >= 7.0 && this.conscientiousness >= 7.0 &&
                this.extraversion >= 7.0 && this.achievement >= 7.0) {
                feature = BigFiveFeature.Generalist;
            }
            else if (this.obligingness >= 4.5 && this.conscientiousness >= 4.5 &&
                    this.extraversion >= 4.5 && this.achievement >= 4.5) {
                feature = BigFiveFeature.Adapter;
            }
            else {
                feature = BigFiveFeature.Expert;
            }
        }

        return feature;
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public double getObligingness() {
        return this.obligingness;
    }

    public double getConscientiousness() {
        return this.conscientiousness;
    }

    public double getExtraversion() {
        return this.extraversion;
    }

    public double getAchievement() {
        return this.achievement;
    }

    public double getNeuroticism() {
        return this.neuroticism;
    }

    public ApproximateValue calcApproximate(double obligingness, double conscientiousness, double extraversion,
                                            double achievement) {
        if (null == this.templateValue) {
            return null;
        }

        ApproximateValue value = new ApproximateValue();

        if (this.templateValue.obligingness >= HighScore && obligingness >= HighScore) {
            value.nearObligingness = true;
        }
        else if (this.templateValue.obligingness <= LowScore && obligingness <= LowScore) {
            value.nearObligingness = true;
        }

        value.deltaObligingness = this.templateValue.obligingness - obligingness;

        if (obligingness < 5.5 && this.templateValue.obligingness < 4.5) {
            value.tendencyObligingness = -1;
        }
        else if (obligingness >= 5.5 && this.templateValue.obligingness > 6.5) {
            value.tendencyObligingness = 1;
        }

        if (this.templateValue.conscientiousness >= HighScore && conscientiousness >= HighScore) {
            value.nearConscientiousness = true;
        }
        else if (this.templateValue.conscientiousness <= LowScore && conscientiousness <= LowScore) {
            value.nearConscientiousness = true;
        }

        value.deltaConscientiousness = this.templateValue.conscientiousness - conscientiousness;

        if (conscientiousness < 5.5 && this.templateValue.conscientiousness < 4.5) {
            value.tendencyConscientiousness = -1;
        }
        else if (conscientiousness >= 5.5 && this.templateValue.conscientiousness > 6.5) {
            value.tendencyConscientiousness = 1;
        }

        if (this.templateValue.extraversion >= HighScore && extraversion >= HighScore) {
            value.nearExtraversion = true;
        }
        else if (this.templateValue.extraversion <= LowScore && extraversion <= LowScore) {
            value.nearExtraversion = true;
        }

        value.deltaExtraversion = this.templateValue.extraversion - extraversion;

        if (extraversion < 5.5 && this.templateValue.extraversion < 4.5) {
            value.tendencyExtraversion = -1;
        }
        else if (extraversion >= 5.5 && this.templateValue.extraversion > 6.5) {
            value.tendencyExtraversion = 1;
        }

        if (this.templateValue.achievement >= HighScore && achievement >= HighScore) {
            value.nearAchievement = true;
        }
        else if (this.templateValue.achievement <= LowScore && achievement <= LowScore) {
            value.nearAchievement = true;
        }

        value.deltaAchievement = this.templateValue.achievement - achievement;

        if (achievement < 5.5 && this.templateValue.achievement < 4.5) {
            value.tendencyAchievement = -1;
        }
        else if (achievement >= 5.5 && this.templateValue.achievement > 6.5) {
            value.tendencyAchievement = 1;
        }

        return value;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }

    public String getObligingnessPrompt() {
        return "宜人性是什么意思？";
    }

    public void setObligingnessParaphrase(String value) {
        if (null == value) {
            return;
        }
        this.obligingnessParaphrase = value;
    }

    public String getConscientiousnessPrompt() {
        return "尽责性是什么意思？";
    }

    public void setConscientiousnessParaphrase(String value) {
        if (null == value) {
            return;
        }
        this.conscientiousnessParaphrase = value;
    }

    public String getExtraversionPrompt() {
        return "外向性是什么意思？";
    }

    public void setExtraversionParaphrase(String value) {
        if (null == value) {
            return;
        }
        this.extraversionParaphrase = value;
    }

    public String getAchievementPrompt() {
        return "进取性是什么意思？";
    }

    public void setAchievementParaphrase(String value) {
        if (null == value) {
            return;
        }
        this.achievementParaphrase = value;
    }

    public String getNeuroticismPrompt() {
        return "情绪性是什么意思？";
    }

    public void setNeuroticismParaphrase(String value) {
        if (null == value) {
            return;
        }
        this.neuroticismParaphrase = value;
    }

    public String generateReportPrompt() {
        return this.displayName + "画像报告";
    }

    public String generateObligingnessPrompt() {
        if (this.obligingness >= HighScore) {
            return "高分宜人性表现";
        }
        else if (this.obligingness <= LowScore) {
            return "低分宜人性表现";
        }
        else {
            return "宜人性一般的表现";
        }
    }

    public void setObligingnessContent(String content) {
        this.obligingnessContent = content;
    }

    public String generateConscientiousnessPrompt() {
        if (this.conscientiousness >= HighScore) {
            return "高分尽责性表现";
        }
        else if (this.conscientiousness <= LowScore) {
            return "低分尽责性表现";
        }
        else {
            return "尽责性一般的表现";
        }
    }

    public void setConscientiousnessContent(String content) {
        this.conscientiousnessContent = content;
    }

    public String generateExtraversionPrompt() {
        if (this.extraversion >= HighScore) {
            return "高分外向性表现";
        }
        else if (this.extraversion <= LowScore) {
            return "低分外向性表现";
        }
        else {
            return "外向性一般的表现";
        }
    }

    public void setExtraversionContent(String content) {
        this.extraversionContent = content;
    }

    public String generateAchievementPrompt() {
        if (this.achievement >= HighScore) {
            return "高分进取性表现";
        }
        else if (this.achievement <= LowScore) {
            return "低分进取性表现";
        }
        else {
            return "进取性一般的表现";
        }
    }

    public void setAchievementContent(String content) {
        this.achievementContent = content;
    }

    public String generateNeuroticismPrompt() {
        if (this.neuroticism >= HighScore) {
            return "高分情绪性表现";
        }
        else if (this.neuroticism <= LowScore) {
            return "低分情绪性表现";
        }
        else {
            return "情绪性一般的表现";
        }
    }

    public void setNeuroticismContent(String content) {
        this.neuroticismContent = content;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("displayName", this.displayName);
        json.put("description", this.description);

        JSONArray scoreArray = new JSONArray();

        JSONObject obligingness = new JSONObject();
        obligingness.put("name", BigFivePersonality.Obligingness.name);
        obligingness.put("code", BigFivePersonality.Obligingness.code);
        obligingness.put("paraphrase", this.obligingnessParaphrase);
        obligingness.put("score", this.obligingness);
        obligingness.put("content", this.obligingnessContent);
        obligingness.put("annotation", this.obligingnessAnnotation.toJSON());
        scoreArray.put(obligingness);

        JSONObject conscientiousness = new JSONObject();
        conscientiousness.put("name", BigFivePersonality.Conscientiousness.name);
        conscientiousness.put("code", BigFivePersonality.Conscientiousness.code);
        conscientiousness.put("paraphrase", this.conscientiousnessParaphrase);
        conscientiousness.put("score", this.conscientiousness);
        conscientiousness.put("content", this.conscientiousnessContent);
        conscientiousness.put("annotation", this.conscientiousnessAnnotation.toJSON());
        scoreArray.put(conscientiousness);

        JSONObject extraversion = new JSONObject();
        extraversion.put("name", BigFivePersonality.Extraversion.name);
        extraversion.put("code", BigFivePersonality.Extraversion.code);
        extraversion.put("paraphrase", this.extraversionParaphrase);
        extraversion.put("score", this.extraversion);
        extraversion.put("content", this.extraversionContent);
        extraversion.put("annotation", this.extraversionAnnotation.toJSON());
        scoreArray.put(extraversion);

        JSONObject achievement = new JSONObject();
        achievement.put("name", BigFivePersonality.Achievement.name);
        achievement.put("code", BigFivePersonality.Achievement.code);
        achievement.put("paraphrase", this.achievementParaphrase);
        achievement.put("score", this.achievement);
        achievement.put("content", this.achievementContent);
        achievement.put("annotation", this.achievementAnnotation.toJSON());
        scoreArray.put(achievement);

        JSONObject neuroticism = new JSONObject();
        neuroticism.put("name", BigFivePersonality.Neuroticism.name);
        neuroticism.put("code", BigFivePersonality.Neuroticism.code);
        neuroticism.put("paraphrase", this.neuroticismParaphrase);
        neuroticism.put("score", this.neuroticism);
        neuroticism.put("content", this.neuroticismContent);
        neuroticism.put("annotation", this.neuroticismAnnotation.toJSON());
        scoreArray.put(neuroticism);

        json.put("scores", scoreArray);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    public TemplateValue getTemplateValue() {
        return this.templateValue;
    }

    public static BigFiveFeature match(String nameOrDisplayName) {
        BigFiveFeature[] features = new BigFiveFeature[] {
                Generalist,
                Advocate,
                Entrepreneur,
                Traditionalist,
                Developer,
                Promoter,
                Realist,
                Idealist,
                Instructor,
                Demonstrator,
                Guide,
                Architect,
                Explorer,
                Supporter,
                Controller,
                Expert,
                Adapter
        };
        for (BigFiveFeature feature : features) {
            if (feature.name.equalsIgnoreCase(nameOrDisplayName)
                    || feature.displayName.equalsIgnoreCase(nameOrDisplayName)) {
                return feature;
            }
        }
        return null;
    }


    public class ScoreAnnotation {

        protected String[] highAdvantages;

        protected String[] highDisadvantages;

        protected String[] lowAdvantages;

        protected String[] lowDisadvantages;

        public ScoreAnnotation(String[] highAdvantages, String[] highDisadvantages,
                               String[] lowAdvantages, String[] lowDisadvantages) {
            this.highAdvantages = highAdvantages;
            this.highDisadvantages = highDisadvantages;
            this.lowAdvantages = lowAdvantages;
            this.lowDisadvantages = lowDisadvantages;
        }

        public ScoreAnnotation(JSONObject json) {
            this.highAdvantages = JSONUtils.toStringArray(json.getJSONArray("highAdvantages"));
            this.highDisadvantages = JSONUtils.toStringArray(json.getJSONArray("highDisadvantages"));
            this.lowAdvantages = JSONUtils.toStringArray(json.getJSONArray("lowAdvantages"));
            this.lowDisadvantages = JSONUtils.toStringArray(json.getJSONArray("lowDisadvantages"));
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("highAdvantages", JSONUtils.toStringArray(this.highAdvantages));
            json.put("highDisadvantages", JSONUtils.toStringArray(this.highDisadvantages));
            json.put("lowAdvantages", JSONUtils.toStringArray(this.lowAdvantages));
            json.put("lowDisadvantages", JSONUtils.toStringArray(this.lowDisadvantages));
            return json;
        }
    }


    public static class TemplateValue {
        /**
         * 宜人性。
         */
        private double obligingness;

        /**
         * 尽责性。
         */
        private double conscientiousness;

        /**
         * 外向性。
         */
        private double extraversion;

        /**
         * 进取性。
         */
        private double achievement;

        /**
         * 情绪性。
         */
        private double neuroticism;

        private Point centroid;

        private TemplateValue(double obligingness, double conscientiousness,
                              double extraversion, double achievement, double neuroticism) {
            this.obligingness = obligingness;
            this.conscientiousness = conscientiousness;
            this.extraversion = extraversion;
            this.achievement = achievement;
            this.neuroticism = neuroticism;

            Point p1 = new Point(10 - obligingness, 10);
            Point p2 = new Point(10, 10 + conscientiousness);
            Point p3 = new Point(10 + extraversion, 10);
            Point p4 = new Point(10, 10 - achievement);

            this.centroid = Point.getCentroid(new Point[] {
                    p1, p2, p3, p4
            });
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append("obligingness: ").append(this.obligingness).append("\n");
            buf.append("conscientiousness: ").append(this.conscientiousness).append("\n");
            buf.append("extraversion: ").append(this.extraversion).append("\n");
            buf.append("achievement: ").append(this.achievement).append("\n");
            buf.append("neuroticism: ").append(this.neuroticism);
            return buf.toString();
        }
    }

    private class ApproximateValue {

        protected boolean nearObligingness = false;
        protected boolean nearConscientiousness = false;
        protected boolean nearExtraversion = false;
        protected boolean nearAchievement = false;

        protected int tendencyObligingness = 0;
        protected int tendencyConscientiousness = 0;
        protected int tendencyExtraversion = 0;
        protected int tendencyAchievement = 0;

        protected double deltaObligingness = 0;
        protected double deltaConscientiousness = 0;
        protected double deltaExtraversion = 0;
        protected double deltaAchievement = 0;

        protected ApproximateValue() {
        }

        public int getNumOfNearValues() {
            int num = 0;
            if (this.nearObligingness) {
                num += 1;
            }
            if (this.nearConscientiousness) {
                num += 1;
            }
            if (this.nearExtraversion) {
                num += 1;
            }
            if (this.nearAchievement) {
                num += 1;
            }
            return num;
        }

        public int getNumOfTendency() {
            int num = 0;
            if (0 != this.tendencyObligingness) {
                num += 1;
            }
            if (0 != this.tendencyConscientiousness) {
                num += 1;
            }
            if (0 != this.tendencyExtraversion) {
                num += 1;
            }
            if (0 != this.tendencyAchievement) {
                num += 1;
            }
            return num;
        }
    }
}
