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

    private int representationTopN;

    private ScoreGroup scoreGroup;

    public EvaluationReport(Attribute attribute, EvaluationFeature evaluationFeature) {
        this(attribute, Collections.singletonList((evaluationFeature)));
    }

    public EvaluationReport(Attribute attribute, List<EvaluationFeature> evaluationFeatureList) {
        this.attribute = attribute;
        this.representationList = new ArrayList<>();
        this.representationTopN = 10;
        this.scoreGroup = new ScoreGroup();
        this.build(evaluationFeatureList);
    }

    public EvaluationReport(JSONObject json) {
        this.attribute = new Attribute(json.getJSONObject("attribute"));
        this.representationList = new ArrayList<>();
        JSONArray array = json.getJSONArray("representationList");
        for (int i = 0; i < array.length(); ++i) {
            this.representationList.add(new Representation(array.getJSONObject(i)));
        }
        this.scoreGroup = new ScoreGroup(json.getJSONObject("scoreGroup"));
    }

    public void setTopN(int value) {
        this.representationTopN = value;
    }

    public boolean isEmpty() {
        return this.representationList.isEmpty();
    }

    public Attribute getAttribute() {
        return this.attribute;
    }

    public ScoreGroup getScoreGroup() {
        return this.scoreGroup;
    }

    public boolean isAbnormal() {
        for (EvaluationScore es : this.scoreGroup.getEvaluationScores()) {
            switch (es.indicator) {
                case Psychosis:
                    if (es.positiveScore > 0.3) {
                        return true;
                    }
                    break;
                case SocialAdaptability:
                    if (es.negativeScore > es.positiveScore) {
                        return true;
                    }
                    break;
                case Depression:
                    if (es.positiveScore > es.negativeScore) {
                        return true;
                    }
                    break;
                default:
                    break;
            }
        }
        return false;
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
                this.scoreGroup.addScore(score);
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

    public List<Representation> getRepresentationListOrderByCorrelation() {
        List<Representation> result = new ArrayList<>(this.representationTopN);
        for (int i = 0, len = Math.min(this.representationTopN, this.representationList.size()); i < len; ++i) {
            result.add(this.representationList.get(i));
        }
        return result;
    }

    /**
     * 通过和评分结果进行对比排序，返回表征列表。
     *
     * @return
     */
    public List<Representation> getRepresentationListByEvaluationScore() {
        List<Representation> result = new ArrayList<>();
        for (Representation representation : this.representationList) {
            // 匹配和评分表一致的特征
            Indicator indicator = this.matchIndicator(representation.knowledgeStrategy.getComment());
            if (null != indicator) {
                result.add(representation);
            }
        }

        if (result.size() == this.representationTopN) {
            return result;
        }

        // 补齐 Top N 数量
        if (result.size() < this.representationTopN) {
            for (Representation representation : this.representationList) {
                if (result.contains(representation)) {
                    continue;
                }

                result.add(representation);
                if (result.size() >= this.representationTopN) {
                    break;
                }
            }
        }
        else {
            while (result.size() > this.representationTopN) {
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
    public List<EvaluationScore> getEvaluationScoresByRepresentation() {
        List<Indicator> indicators = new ArrayList<>();
        for (Representation representation : this.representationList) {
            // 匹配和评分表一致的特征
            Indicator indicator = this.matchIndicator(representation.knowledgeStrategy.getComment());
            if (null != indicator) {
                indicators.add(indicator);
            }
        }

        List<EvaluationScore> list = this.scoreGroup.getEvaluationScores();
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

        return result;
    }

    public List<EvaluationScore> getEvaluationScores() {
        return this.scoreGroup.getEvaluationScores();
    }

    public int numEvaluationScores() {
        return this.scoreGroup.getEvaluationScores().size();
    }

    private Indicator matchIndicator(Comment comment) {
        switch (comment) {
            case Extroversion:
                return Indicator.Extroversion;
            case Introversion:
                return Indicator.Introversion;
            case Narcissism:
                return Indicator.Narcissism;
            case SelfConfidence:
                return Indicator.Confidence;
            case SelfEsteem:
                return Indicator.SelfEsteem;
            case SocialAdaptability:
                return Indicator.SocialAdaptability;
            case Independence:
                return Indicator.Independence;
            case Idealization:
                return Indicator.Idealism;
            case DelicateEmotions:
                return Indicator.Emotion;
            case SelfExistence:
                return Indicator.SelfConsciousness;
            case Luxurious:
                return Indicator.Realism;
            case SenseOfSecurity:
                return Indicator.SenseOfSecurity;
            case HighPressure:
            case ExternalPressure:
                return Indicator.Stress;
            case SelfControl:
                return Indicator.SelfControl;
            case WorldWeariness:
            case Escapism:
                return Indicator.Anxiety;
            case Depression:
                return Indicator.Depression;
            case SimpleIdea:
            case Childish:
                return Indicator.Simple;
            case Hostility:
                return Indicator.Hostile;
            case Aggression:
                return Indicator.Attacking;
            case PayAttentionToFamily:
                return Indicator.Family;
            case PursueInterpersonalRelationships:
                return Indicator.InterpersonalRelation;
            case PursuitOfAchievement:
                return Indicator.AchievementMotivation;
            case Creativity:
                return Indicator.Creativity;
            case PositiveExpectation:
                return Indicator.Struggle;
            case DesireForFreedom:
                return Indicator.DesireForFreedom;
            default:
                break;
        }
        return null;
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

        json.put("scoreGroup", this.scoreGroup.toJSON());

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

        json.put("scoreGroup", this.scoreGroup.toCompactJSON());

        return json;
    }
}
