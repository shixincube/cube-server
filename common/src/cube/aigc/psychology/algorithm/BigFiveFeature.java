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

import cube.aigc.psychology.composition.TheBigFive;
import cube.common.JSONable;
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

    private String obligingnessContent = "";

    /**
     * 尽责性。
     */
    private double conscientiousness;

    private String conscientiousnessContent = "";

    /**
     * 外向性。
     */
    private double extraversion;

    private String extraversionContent = "";

    /**
     * 进取性。
     */
    private double achievement;

    private String achievementContent = "";

    /**
     * 情绪性。
     */
    private double neuroticism;

    private String neuroticismContent = "";

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
            String content = factor.has("content") ? factor.getString("content") : "";
            switch (TheBigFive.parse(code)) {
                case Obligingness:
                    this.obligingness = score;
                    this.obligingnessContent = content;
                    break;
                case Conscientiousness:
                    this.conscientiousness = score;
                    this.conscientiousnessContent = content;
                    break;
                case Extraversion:
                    this.extraversion = score;
                    this.extraversionContent = content;
                    break;
                case Achievement:
                    this.achievement = score;
                    this.achievementContent = content;
                    break;
                case Neuroticism:
                    this.neuroticism = score;
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
        Point centroid = getCentroid(new Point[] {
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
        obligingness.put("name", TheBigFive.Obligingness.name);
        obligingness.put("code", TheBigFive.Obligingness.code);
        obligingness.put("score", this.obligingness);
        obligingness.put("content", this.obligingnessContent);
        scoreArray.put(obligingness);

        JSONObject conscientiousness = new JSONObject();
        conscientiousness.put("name", TheBigFive.Conscientiousness.name);
        conscientiousness.put("code", TheBigFive.Conscientiousness.code);
        conscientiousness.put("score", this.conscientiousness);
        conscientiousness.put("content", this.conscientiousnessContent);
        scoreArray.put(conscientiousness);

        JSONObject extraversion = new JSONObject();
        extraversion.put("name", TheBigFive.Extraversion.name);
        extraversion.put("code", TheBigFive.Extraversion.code);
        extraversion.put("score", this.extraversion);
        extraversion.put("content", this.extraversionContent);
        scoreArray.put(extraversion);

        JSONObject achievement = new JSONObject();
        achievement.put("name", TheBigFive.Achievement.name);
        achievement.put("code", TheBigFive.Achievement.code);
        achievement.put("score", this.achievement);
        achievement.put("content", this.achievementContent);
        scoreArray.put(achievement);

        JSONObject neuroticism = new JSONObject();
        neuroticism.put("name", TheBigFive.Neuroticism.name);
        neuroticism.put("code", TheBigFive.Neuroticism.code);
        neuroticism.put("score", this.neuroticism);
        neuroticism.put("content", this.neuroticismContent);
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

    private static Point getCentroid(Point[] points) {
        double totalArea = 0;
        double totalX = 0;
        double totalY = 0;

        for (int i = 0; i < points.length; ++i) {
            if (i + 1 >= points.length) {
                break;
            }

            Point a = points[i + 1];
            Point b = points[i];

            double area = 0.5 * (a.x * b.y - b.x * a.y);    // 计算面积
            double x = (a.x + b.x) / 3.0;   // x 方向质心
            double y = (a.y + b.y) / 3.0;   // y 方向质心

            totalArea += area;
            totalX += area * x;
            totalY += area * y;
        }

        return new Point(totalX / totalArea, totalY / totalArea);
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

            this.centroid = getCentroid(new Point[] {
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
