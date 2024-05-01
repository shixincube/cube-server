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

    private void build(List<EvaluationFeature> resultList) {
        for (EvaluationFeature result : resultList) {
            for (EvaluationFeature.Feature feature : result.getFeatures()) {
                Representation representation = this.getRepresentation(feature.comment);
                if (null == representation) {
                    KnowledgeStrategy interpretation = Resource.getInstance().getCommentInterpretation(feature.comment);
                    if (null == interpretation) {
                        // 没有对应的释义
                        Logger.e(this.getClass(), "#build - Can NOT find comment interpretation: " + feature.comment.word);
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
    }

    private void calcAttentionSuggestion() {
        int score = 0;
        boolean depression = false;
        boolean senseOfSecurity = false;

        for (EvaluationScore es : this.scoreAccelerator.getEvaluationScores()) {
            switch (es.indicator) {
                case Psychosis:
                    if (es.positiveScore > 0.3) {
                        score += 4;
                    }
                    else if (es.positiveScore > 0.2) {
                        score += 3;
                    }
                    break;
                case SocialAdaptability:
                    // 负分应当大于 0.2
                    if (es.negativeScore > es.positiveScore && es.negativeScore >= 0.2) {
                        score += 1;

                        if (es.negativeScore - es.positiveScore > 0.3) {
                            score += 1;
                        }
                    }
                    break;
                case Depression:
                    if (es.positiveScore > es.negativeScore) {
                        depression = true;
                        score += 1;

                        if (es.positiveScore > 0.6) {
                            score += 2;
                        }
                    }
                    break;
                case SenseOfSecurity:
                    if (es.negativeScore > 0.5) {
                        senseOfSecurity = true;
                        score += 1;
                    }
                    break;
                default:
                    break;
            }
        }

        if (depression && senseOfSecurity) {
            score += 1;
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

    public Representation getRepresentation(Comment comment) {
        for (Representation representation : this.representationList) {
            if (representation.knowledgeStrategy.getComment() == comment) {
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
            Indicator indicator = RepresentationStrategy.matchIndicator(representation.knowledgeStrategy.getComment());
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

        return result;
    }

    /**
     * 通过和表征对比、排序，返回评估得分。
     *
     * @return
     */
    public List<EvaluationScore> getEvaluationScoresByRepresentation(int topNum) {
        List<Indicator> indicators = new ArrayList<>();
        for (Representation representation : this.representationList) {
            // 匹配和评分表一致的特征
            Indicator indicator = RepresentationStrategy.matchIndicator(representation.knowledgeStrategy.getComment());
            if (null != indicator) {
                indicators.add(indicator);
            }
        }

        List<EvaluationScore> list = this.scoreAccelerator.getEvaluationScores();
        List<EvaluationScore> result = new ArrayList<>();
        for (EvaluationScore es : list) {
            if (indicators.contains(es.indicator)) {
                result.add(es);
            }
        }

        Collections.sort(result, new Comparator<EvaluationScore>() {
            @Override
            public int compare(EvaluationScore es1, EvaluationScore es2) {
                return es2.hit - es1.hit;
            }
        });

        while (result.size() > topNum) {
            result.remove(result.size() - 1);
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

        JSONArray priorityArray = new JSONArray();
        for (EvaluationScore es : this.getEvaluationScoresByRepresentation(100)) {
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

        JSONArray priorityArray = new JSONArray();
        for (EvaluationScore es : this.getEvaluationScoresByRepresentation(100)) {
            priorityArray.put(es.toCompactJSON());
        }
        json.put("priorities", priorityArray);

        return json;
    }
}
