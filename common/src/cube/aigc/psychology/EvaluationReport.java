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

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.algorithm.*;
import cube.aigc.psychology.composition.EvaluationScore;
import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 评估报告。
 */
public class EvaluationReport implements JSONable {

    /**
     * 问题检出时依赖的关键指标。
     */
    public final Indicator[] KEY_INDICATORS = new Indicator[] {
            Indicator.Psychosis,
            Indicator.SocialAdaptability,
            Indicator.Depression
    };

    public final static String UNIT = ModelConfig.BAIZE_UNIT;

    private Attribute attribute;

    private List<Representation> representationList;

    private ScoreAccelerator scoreAccelerator;

    private AttentionSuggestion attentionSuggestion;

    private boolean hesitating = false;

    public EvaluationReport(Attribute attribute, EvaluationFeature evaluationFeature) {
        this(attribute, Collections.singletonList((evaluationFeature)));
    }

    public EvaluationReport(Attribute attribute, List<EvaluationFeature> evaluationFeatureList) {
        this.attribute = attribute;
        this.representationList = new ArrayList<>();
        this.scoreAccelerator = new ScoreAccelerator();
        this.attentionSuggestion = AttentionSuggestion.NoAttention;
        this.build(evaluationFeatureList);
    }

    public EvaluationReport(JSONObject json) {
        this.attribute = new Attribute(json.getJSONObject("attribute"));
        this.representationList = new ArrayList<>();
        JSONArray array = json.getJSONArray("representationList");
        for (int i = 0; i < array.length(); ++i) {
            this.representationList.add(new Representation(array.getJSONObject(i)));
        }
        this.scoreAccelerator = new ScoreAccelerator(json.getJSONObject("accelerator"));
        this.attentionSuggestion = AttentionSuggestion.parse(json.getInt("attention"));
        this.hesitating = json.has("hesitating") && json.getBoolean("hesitating");
    }

    public boolean isEmpty() {
        return this.representationList.isEmpty();
    }

    public Attribute getAttribute() {
        return this.attribute;
    }

    public ScoreAccelerator getScoreAccelerator() {
        return this.scoreAccelerator;
    }

    public AttentionSuggestion getAttentionSuggestion() {
        return this.attentionSuggestion;
    }

    public boolean isHesitating() {
        return this.hesitating;
    }

    private void build(List<EvaluationFeature> resultList) {
        for (EvaluationFeature result : resultList) {
            for (EvaluationFeature.Feature feature : result.getFeatures()) {
                Representation representation = this.getRepresentation(feature.term);
                if (null == representation) {
                    KnowledgeStrategy interpretation = Resource.getInstance().getTermInterpretation(feature.term);
                    if (null == interpretation) {
                        // 没有对应的释义
                        Logger.e(this.getClass(), "#build - Can NOT find comment interpretation: " + feature.term.word);
                        continue;
                    }

                    representation = new Representation(interpretation);
                    this.representationList.add(representation);
                }

                if (feature.tendency == Tendency.Negative) {
                    representation.negativeCorrelation += 1;
                }
                if (feature.tendency == Tendency.Positive) {
                    representation.positiveCorrelation += 1;
                }
            }

            // 提取并合并分数
            for (Score score : result.getScores()) {
                this.scoreAccelerator.addScore(score);
            }
        }

        // 排序
        this.representationList.sort(new Comparator<Representation>() {
            @Override
            public int compare(Representation representation1, Representation representation2) {
                int score1 = representation1.positiveCorrelation + representation1.negativeCorrelation;
                int score2 = representation2.positiveCorrelation + representation2.negativeCorrelation;
                return score2 - score1;
            }
        });

        // 计算关注
        this.calcAttentionSuggestion();

        // 判断 hesitating
        if (this.representationList.size() <= 7 || this.scoreAccelerator.getEvaluationScores().size() <= 5) {
            this.hesitating = true;
        }
    }

    private void calcAttentionSuggestion() {
        int score = 0;
        boolean depression = false;
        double depressionScore = 0;
        boolean senseOfSecurity = false;
        boolean stress = false;
        boolean anxiety = false;
        boolean optimism = false;
        boolean pessimism = false;
        boolean obsession = false;

        for (EvaluationScore es : this.scoreAccelerator.getEvaluationScores()) {
            switch (es.indicator) {
                case Psychosis:
                    if (es.positiveScore > 0.8) {
                        score += 5;
                    }
                    else if (es.positiveScore > 0.4) {
                        score += 4;
                    }
                    else if (es.positiveScore > 0.2) {
                        score += 3;
                    }
                    break;
                case SocialAdaptability:
                    double delta = es.negativeScore - es.positiveScore;
                    if (delta > 0) {
                        if (delta > 0.7) {
                            score += 2;
                        }
                        else if (delta >= 0.2) {
                            score += 1;
                        }
                    }
                    break;
                case Depression:
                    depressionScore = es.positiveScore - es.negativeScore;
                    if (depressionScore >= 0.9) {
                        depression = true;
                        score += 3;
                    }
                    else if (depressionScore > 0.7) {
                        depression = true;
                        score += 2;
                    }
                    else if (depressionScore > 0.5) {
                        depression = true;
                        score += 1;
                    }
                    else if (depressionScore > 0.3) {
                        depression = true;
                        score += 1;
                    }
                    else if (depressionScore >= 0.1) {
                        depression = true;
                    }
                    else if (depressionScore < 0.1) {
                        score -= 1;
                    }
                    break;
                case SenseOfSecurity:
                    if (es.negativeScore - es.positiveScore > 0.6) {
                        senseOfSecurity = true;
                        score += 2;
                    }
                    else if (es.negativeScore - es.positiveScore >= 0.5) {
                        senseOfSecurity = true;
                        score += 1;
                    }
                    break;
                case Stress:
                    // 压力 0.4 是阈值
                    if (es.positiveScore - es.negativeScore > 0.5) {
                        stress = true;
                    }
                    break;
                case Anxiety:
                    if (es.positiveScore - es.negativeScore > 1.1) {
                        anxiety = true;
                        score += 1;
                    }
                    else if (es.positiveScore - es.negativeScore > 0.4) {
                        anxiety = true;
                    }
                    else if (es.positiveScore - es.negativeScore < 0.1) {
                        score -= 1;
                    }
                    break;
                case Obsession:
                    if (es.positiveScore > 0.6) {
                        obsession = true;
                        score += 2;
                    }
                    else if (es.positiveScore > 0.4) {
                        obsession = true;
                        score += 1;
                    }
                    break;
                case Optimism:
                    if (es.positiveScore - es.negativeScore > 1.0) {
                        optimism = true;
                        score -= 1;
                    }
                    else if (es.positiveScore - es.negativeScore > 0.5) {
                        optimism = true;
                        if (depressionScore >= 0.4) {
                            score -= 1;
                        }
                    }
                    break;
                case Pessimism:
                    if (es.positiveScore - es.negativeScore > 0.1) {
                        pessimism = true;
                    }
                    break;
                case Unknown:
                    // 绘图未被识别
                    score = 4;
                    break;
                default:
                    break;
            }
        }

        if (depression && senseOfSecurity) {
            score += 1;
            Logger.d(this.getClass(), "#calcAttentionSuggestion - (depression && senseOfSecurity)");
        }
        if (depression && stress) {
            score += 1;
            Logger.d(this.getClass(), "#calcAttentionSuggestion - (depression && stress)");
        }
        if (depression && anxiety) {
            score += 1;
            Logger.d(this.getClass(), "#calcAttentionSuggestion - (depression && anxiety)");
        }
        if (depression && pessimism) {
            score += 1;
            Logger.d(this.getClass(), "#calcAttentionSuggestion - (depression && pessimism)");
        }

        if (optimism) {
            score -= 1;
        }

        Logger.d(this.getClass(), "#calcAttentionSuggestion - score: " + score + " - " +
                "depression:" + depression + " | " +
                "senseOfSecurity:" + senseOfSecurity + " | " +
                "stress:" + stress + " | " +
                "anxiety:" + anxiety + " | " +
                "obsession:" + obsession + " | " +
                "optimism:" + optimism + " | " +
                "pessimism:" + pessimism);

        // 根据年龄就行修正
        if (this.attribute.age >= 35) {
            score -= 1;
        }

        if (score > 0) {
            if (score >= 5) {
                this.attentionSuggestion = AttentionSuggestion.SpecialAttention;
            }
            else if (score >= 3) {
                this.attentionSuggestion = AttentionSuggestion.FocusedAttention;
            }
            else {
                this.attentionSuggestion = AttentionSuggestion.GeneralAttention;
            }
        }
    }

    public Representation getRepresentation(Term term) {
        for (Representation representation : this.representationList) {
            if (representation.knowledgeStrategy.getTerm() == term) {
                return representation;
            }
        }
        return null;
    }

    public int numRepresentations() {
        return this.representationList.size();
    }

    public List<Representation> getRepresentationList() {
        return this.representationList;
    }

//    public List<Representation> getRepresentationListOrderByCorrelation() {
//        List<Representation> result = new ArrayList<>(this.representationTopN);
//        for (int i = 0, len = Math.min(this.representationTopN, this.representationList.size()); i < len; ++i) {
//            result.add(this.representationList.get(i));
//        }
//        return result;
//    }

    /**
     * 通过和评分结果进行对比排序，返回表征列表。
     *
     * @return
     */
    public List<Representation> getRepresentationListByEvaluationScore(int topNum) {
        List<Representation> result = new ArrayList<>();
        for (Representation representation : this.representationList) {
            // 匹配和评分表一致的特征
            Indicator indicator = RepresentationStrategy.matchIndicator(representation.knowledgeStrategy.getTerm());
            if (null != indicator) {
                result.add(representation);
            }
        }

        if (result.size() == topNum) {
            return result;
        }

        // 补齐 Top N 数量
        if (result.size() < topNum) {
            for (Representation representation : this.representationList) {
                if (result.contains(representation)) {
                    continue;
                }

                result.add(representation);
                if (result.size() >= topNum) {
                    break;
                }
            }
        }
        else {
            while (result.size() > topNum) {
                result.remove(result.size() - 1);
            }
        }

        // "理想化" Idealization
        for (Representation representation : result) {
            if (representation.knowledgeStrategy.getTerm() == Term.Idealization) {
                result.remove(representation);
                result.add(representation);
                break;
            }
        }

        return result;
    }

    /**
     * 返回特征列表，删除与评估分重叠的数据。
     *
     * @return
     */
    public List<Representation> getRepresentationListWithoutEvaluationScore() {
        List<Representation> result = new ArrayList<>();
        result.addAll(this.representationList);

        for (Representation representation : this.representationList) {
            // 匹配和评分表一致的特征
            Indicator indicator = RepresentationStrategy.matchIndicator(representation.knowledgeStrategy.getTerm());
            if (null != indicator) {
                result.remove(representation);
            }
        }

        // 删除"理想化" Idealization
        for (Representation representation : result) {
            if (representation.knowledgeStrategy.getTerm() == Term.Idealization) {
                result.remove(representation);
                break;
            }
        }

        return result;
    }

    /**
     * 通过和表征对比、排序，返回评估得分。
     *
     * @return
     */
    public List<EvaluationScore> getEvaluationScoresByRepresentation() {
        int num = 10;

        List<Indicator> indicators = new ArrayList<>();
        for (Representation representation : this.representationList) {
            // 匹配和评分表一致的特征
            Indicator indicator = RepresentationStrategy.matchIndicator(representation.knowledgeStrategy.getTerm());
            if (null != indicator) {
                indicators.add(indicator);
            }
        }

        List<EvaluationScore> evaluationScores = this.scoreAccelerator.getEvaluationScores();

        // 按照优先级排序
        Collections.sort(evaluationScores, new Comparator<EvaluationScore>() {
            @Override
            public int compare(EvaluationScore es1, EvaluationScore es2) {
                return es2.indicator.priority - es1.indicator.priority;
            }
        });

        List<EvaluationScore> result = new ArrayList<>();
        for (EvaluationScore es : evaluationScores) {
            if (indicators.contains(es.indicator)) {
                result.add(es);
            }
        }

        if (result.size() < num) {
            // 补充
            for (EvaluationScore es : evaluationScores) {
                if (!result.contains(es)) {
                    result.add(es);
                    if (result.size() >= num) {
                        break;
                    }
                }
            }
        }
        else {
            while (result.size() > num) {
                result.remove(result.size() - 1);
            }
        }

        // "人际关系"移到最后
        if (result.get(0).indicator == Indicator.InterpersonalRelation) {
            EvaluationScore es = result.remove(0);
            result.add(es);
        }
        // "理想主义"移动最后
        for (EvaluationScore es : result) {
            if (es.indicator == Indicator.Idealism) {
                result.remove(es);
                result.add(es);
                break;
            }
        }
        // "自我意识"移动到最后
        for (EvaluationScore es : result) {
            if (es.indicator == Indicator.SelfConsciousness) {
                result.remove(es);
                result.add(es);
                break;
            }
        }

        return result;
    }

    public List<EvaluationScore> getEvaluationScores() {
        return this.scoreAccelerator.getEvaluationScores();
    }

    public int numEvaluationScores() {
        return this.scoreAccelerator.getEvaluationScores().size();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("attribute", this.attribute.toJSON());

        JSONArray array = new JSONArray();
        for (Representation representation : this.representationList) {
            array.put(representation.toJSON());
        }
        json.put("representationList", array);

        json.put("accelerator", this.scoreAccelerator.toJSON());

        json.put("attention", this.attentionSuggestion.level);

        json.put("hesitating", this.hesitating);

        JSONArray priorityArray = new JSONArray();
        for (EvaluationScore es : this.getEvaluationScoresByRepresentation()) {
            priorityArray.put(es.toJSON());
        }
        json.put("priorities", priorityArray);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("attribute", this.attribute.toJSON());

        JSONArray array = new JSONArray();
        for (Representation representation : this.representationList) {
            array.put(representation.toCompactJSON());
        }
        json.put("representationList", array);

        json.put("accelerator", this.scoreAccelerator.toCompactJSON());

        json.put("attention", this.attentionSuggestion.level);

        json.put("hesitating", this.hesitating);

        JSONArray priorityArray = new JSONArray();
        for (EvaluationScore es : this.getEvaluationScoresByRepresentation()) {
            priorityArray.put(es.toCompactJSON());
        }
        json.put("priorities", priorityArray);

        return json;
    }
}
