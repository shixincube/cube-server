/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.psychology.Indicator;
import cube.aigc.psychology.algorithm.Attention;
import cube.aigc.psychology.algorithm.IndicatorRate;
import cube.aigc.psychology.algorithm.PaintingConfidence;
import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HexagonDimensionScore implements JSONable {

    private Map<HexagonDimension, Integer> scores = new LinkedHashMap<>();

    private Map<HexagonDimension, String> descriptions = new LinkedHashMap<>();

    private Map<HexagonDimension, Integer> rates = new LinkedHashMap<>();

    public HexagonDimensionScore() {
    }

    public HexagonDimensionScore(int mood, int cognition, int behavior, int interpersonalRelationship,
                                 int selfAssessment, int mentalHealth) {
        this.recordScore(HexagonDimension.Mood, mood, IndicatorRate.Medium);
        this.recordScore(HexagonDimension.Cognition, cognition, IndicatorRate.Medium);
        this.recordScore(HexagonDimension.Behavior, behavior, IndicatorRate.Medium);
        this.recordScore(HexagonDimension.InterpersonalRelationship, interpersonalRelationship, IndicatorRate.Medium);
        this.recordScore(HexagonDimension.SelfAssessment, selfAssessment, IndicatorRate.Medium);
        this.recordScore(HexagonDimension.MentalHealth, mentalHealth, IndicatorRate.Medium);
    }

    public HexagonDimensionScore(Attention attention, List<EvaluationScore> scoreList, PaintingConfidence confidence,
                                 FactorSet factorSet) {
        for (HexagonDimension hd : HexagonDimension.values()) {
            this.recordScore(hd, 80, IndicatorRate.Medium);
        }

        if (null != confidence) {
            // 认知
            switch (confidence.getConfidenceLevel()) {
                case PaintingConfidence.LEVEL_HIGHER:
                    this.recordScore(HexagonDimension.Cognition, Utils.randomInt(80, 89),
                            IndicatorRate.Highest);
                    break;
                case PaintingConfidence.LEVEL_HIGH:
                    this.recordScore(HexagonDimension.Cognition, Utils.randomInt(75, 79),
                            IndicatorRate.High);
                    break;
                case PaintingConfidence.LEVEL_NORMAL:
                    this.recordScore(HexagonDimension.Cognition, Utils.randomInt(70, 74),
                            IndicatorRate.Medium);
                    break;
                case PaintingConfidence.LEVEL_LOW:
                    this.recordScore(HexagonDimension.Cognition, Utils.randomInt(60, 69),
                            IndicatorRate.Low);
                    break;
                case PaintingConfidence.LEVEL_LOWER:
                    this.recordScore(HexagonDimension.Cognition, Utils.randomInt(50, 59),
                            IndicatorRate.Lowest);
                    break;
                default:
                    break;
            }
        }
        else {
            Logger.w(this.getClass(), "PaintingConfidence is null");
        }

        if (null != factorSet) {
            // 情绪
            if (factorSet.affectFactor.positive > factorSet.affectFactor.negative) {
                if (attention == Attention.SpecialAttention || attention == Attention.FocusedAttention) {
                    if (factorSet.affectFactor.positive - factorSet.affectFactor.negative > 10) {
                        this.recordScore(HexagonDimension.Mood, Utils.randomInt(70, 79),
                                IndicatorRate.High);
                    }
                    else {
                        this.recordScore(HexagonDimension.Mood, Utils.randomInt(60, 69),
                                IndicatorRate.Medium);
                    }
                }
                else {
                    if (factorSet.affectFactor.positive - factorSet.affectFactor.negative > 10) {
                        this.recordScore(HexagonDimension.Mood, Utils.randomInt(80, 89),
                                IndicatorRate.Highest);
                    }
                    else {
                        this.recordScore(HexagonDimension.Mood, Utils.randomInt(70, 79),
                                IndicatorRate.High);
                    }
                }
            }
            else {
                if (factorSet.affectFactor.negative - factorSet.affectFactor.positive > 10) {
                    this.recordScore(HexagonDimension.Mood, Utils.randomInt(50, 59),
                            IndicatorRate.Low);
                }
                else {
                    this.recordScore(HexagonDimension.Mood, Utils.randomInt(60, 69),
                            IndicatorRate.Medium);
                }
            }

            // 心理健康
            if (attention == Attention.SpecialAttention) {
                this.recordScore(HexagonDimension.MentalHealth, Utils.randomInt(40, 49),
                        IndicatorRate.Lowest);
            }
            else if (attention == Attention.FocusedAttention) {
                if (factorSet.calcSymptomTotal() > 160) {
                    this.recordScore(HexagonDimension.MentalHealth, Utils.randomInt(50, 54),
                            IndicatorRate.Low);
                }
                else {
                    if (factorSet.normDepression().norm && factorSet.normSomatization().norm && factorSet.normAnxiety().norm) {
                        this.recordScore(HexagonDimension.MentalHealth, Utils.randomInt(60, 69),
                                IndicatorRate.Medium);
                    }
                    else {
                        this.recordScore(HexagonDimension.MentalHealth, Utils.randomInt(55, 59),
                                IndicatorRate.Low);
                    }
                }
            }
            else if (attention == Attention.GeneralAttention) {
                if (factorSet.calcSymptomTotal() > 160) {
                    this.recordScore(HexagonDimension.MentalHealth, Utils.randomInt(70, 74),
                            IndicatorRate.High);
                }
                else {
                    if (factorSet.normDepression().norm && factorSet.normSomatization().norm && factorSet.normAnxiety().norm) {
                        this.recordScore(HexagonDimension.MentalHealth, Utils.randomInt(78, 79),
                                IndicatorRate.High);
                    }
                    else {
                        this.recordScore(HexagonDimension.MentalHealth, Utils.randomInt(75, 77),
                                IndicatorRate.Medium);
                    }
                }
            }
            else {
                if (factorSet.calcSymptomTotal() > 160) {
                    this.recordScore(HexagonDimension.MentalHealth, Utils.randomInt(80, 84),
                            IndicatorRate.High);
                }
                else {
                    if (factorSet.normDepression().norm && factorSet.normSomatization().norm && factorSet.normAnxiety().norm) {
                        this.recordScore(HexagonDimension.MentalHealth, Utils.randomInt(88, 89),
                                IndicatorRate.Highest);
                    }
                    else {
                        this.recordScore(HexagonDimension.MentalHealth, Utils.randomInt(85, 87),
                                IndicatorRate.High);
                    }
                }
            }

            // 人际敏感
            if (factorSet.symptomFactor.interpersonal > 3.0) {
                this.recordScore(HexagonDimension.InterpersonalRelationship, Utils.randomInt(80, 89),
                        IndicatorRate.High);
            }
            else if (factorSet.symptomFactor.interpersonal > 2.5) {
                this.recordScore(HexagonDimension.InterpersonalRelationship, Utils.randomInt(70, 79),
                        IndicatorRate.Medium);
            }
            else if (factorSet.symptomFactor.interpersonal > 1.66) {
                this.recordScore(HexagonDimension.InterpersonalRelationship, Utils.randomInt(60, 69),
                        IndicatorRate.Low);
            }
            else {
                this.recordScore(HexagonDimension.InterpersonalRelationship, Utils.randomInt(55, 59),
                        IndicatorRate.Low);
            }

            // 行为
            if (factorSet.normHostile().norm && factorSet.normHorror().norm && factorSet.normParanoid().norm) {
                this.recordScore(HexagonDimension.Behavior, Utils.randomInt(80, 89),
                        IndicatorRate.Highest);
            }
            else if (factorSet.normHostile().norm && factorSet.normParanoid().norm) {
                this.recordScore(HexagonDimension.Behavior, Utils.randomInt(70, 79),
                        IndicatorRate.High);
            }
            else if (factorSet.symptomFactor.hostile <= 2.5 && factorSet.symptomFactor.paranoid <= 2.5) {
                this.recordScore(HexagonDimension.Behavior, Utils.randomInt(60, 69),
                        IndicatorRate.Medium);
            }
            else {
                this.recordScore(HexagonDimension.Behavior, Utils.randomInt(50, 59),
                        IndicatorRate.Low);
            }

            // 自我评价
            EvaluationScore selfConsciousness = null;
            EvaluationScore socialAdaptability = null;
            EvaluationScore idealism = null;
            EvaluationScore realism = null;
            for (EvaluationScore es : scoreList) {
                if (es.indicator == Indicator.SelfConsciousness) {
                    selfConsciousness = es;
                }
                else if (es.indicator == Indicator.SocialAdaptability) {
                    socialAdaptability = es;
                }
                else if (es.indicator == Indicator.Idealism) {
                    idealism = es;
                }
                else if (es.indicator == Indicator.Realism) {
                    realism = es;
                }
            }
            if (attention == Attention.FocusedAttention || attention == Attention.SpecialAttention) {
                this.recordScore(HexagonDimension.SelfAssessment, Utils.randomInt(60, 69),
                        IndicatorRate.Medium);
            }
            else {
                if (selfConsciousness.calcScore() > 0 && socialAdaptability.calcScore() > 0 && realism.calcScore() > idealism.calcScore()) {
                    this.recordScore(HexagonDimension.SelfAssessment, Utils.randomInt(80, 89),
                            IndicatorRate.High);
                }
                else {
                    this.recordScore(HexagonDimension.SelfAssessment, Utils.randomInt(70, 79),
                            IndicatorRate.Medium);
                }
            }
        }
        else {
            Logger.w(this.getClass(), "FactorSet is null");
        }
    }

    public HexagonDimensionScore(JSONObject json) {
        if (json.has("factors") && json.has("scores") && json.has("descriptions")) {
            // 新结构
            JSONArray factors = json.getJSONArray("factors");
            JSONArray scores = json.getJSONArray("scores");
            JSONArray descriptions = json.getJSONArray("descriptions");
            JSONArray rates = json.getJSONArray("rates");
            for (int i = 0; i < factors.length(); ++i) {
                String factor = factors.getString(i);
                int score = scores.getInt(i);
                String description = descriptions.getString(i);
                int rate = rates.getInt(i);
                HexagonDimension hexagonDimension = HexagonDimension.parse(factor);
                this.scores.put(hexagonDimension, score);
                this.descriptions.put(hexagonDimension, description);
                this.rates.put(hexagonDimension, rate);
            }
        }
        else {
            // 旧结构
            Iterator<String> iter = json.keys();
            while (iter.hasNext()) {
                String key = iter.next();
                HexagonDimension hexagonDimension = HexagonDimension.parse(key);
                if (null == hexagonDimension) {
                    continue;
                }
                int value = json.getInt(key);
                this.scores.put(hexagonDimension, value);
            }
        }
    }

    public void recordScore(HexagonDimension dim, int value, IndicatorRate rate) {
        this.scores.put(dim, value);
        this.rates.put(dim, rate.value);
    }

    public void recordDescription(HexagonDimension dim, String description) {
        this.descriptions.put(dim, description);
    }

    public int getDimensionScore(HexagonDimension hexagonDimension) {
        if (!this.scores.containsKey(hexagonDimension)) {
            return 0;
        }
        return this.scores.get(hexagonDimension);
    }

    public int getDimensionRate(HexagonDimension hexagonDimension) {
        if (!this.rates.containsKey(hexagonDimension)) {
            return 0;
        }
        return this.rates.get(hexagonDimension);
    }

    public String getDimensionDescription(HexagonDimension hexagonDimension) {
        return this.descriptions.get(hexagonDimension);
    }

    public void setFactor(JSONObject hexagonFactorJson) {
        HexagonFactor hexagonFactor = new HexagonFactor(hexagonFactorJson);
        this.scores.put(hexagonFactor.hexagonDimension, hexagonFactor.score);
        this.descriptions.put(hexagonFactor.hexagonDimension, hexagonFactor.description);
        this.rates.put(hexagonFactor.hexagonDimension, hexagonFactor.rate);
    }

    public HexagonFactor getFactor(HexagonDimension hexagonDimension) {
        return new HexagonFactor(hexagonDimension, this.scores.get(hexagonDimension),
                this.descriptions.get(hexagonDimension),
                this.rates.get(hexagonDimension));
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        // 旧结构
        for (Map.Entry<HexagonDimension, Integer> e : this.scores.entrySet()) {
            json.put(e.getKey().name, e.getValue().intValue());
//            if (e.getKey() == HexagonDimension.Mood) {
//                json.put("Emotion", e.getValue().intValue());
//            }
        }

        // 新结构
        JSONArray factors = new JSONArray();
        JSONArray scores = new JSONArray();
        JSONArray descriptions = new JSONArray();
        JSONArray rates = new JSONArray();
        try {
            for (Map.Entry<HexagonDimension, Integer> e : this.scores.entrySet()) {
                factors.put(e.getKey().name);
                scores.put(e.getValue().intValue());

                String desc = this.descriptions.get(e.getKey());
                if (null != desc) {
                    descriptions.put(desc);

                    rates.put(this.rates.get(e.getKey()));
                }
            }
        } catch (Exception e) {
            Logger.e(this.getClass(), "#toJSON", e);
        }
        json.put("factors", factors);
        json.put("scores", scores);
        if (descriptions.length() > 0) {
            json.put("descriptions", descriptions);
            json.put("rates", rates);
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    public class HexagonFactor {

        public final HexagonDimension hexagonDimension;

        public final int score;

        public final String description;

        public final int rate;

        public HexagonFactor(HexagonDimension hexagonDimension, int score, String description, int rate) {
            this.hexagonDimension = hexagonDimension;
            this.score = score;
            this.description = description;
            this.rate = rate;
        }

        public HexagonFactor(JSONObject json) {
            this.hexagonDimension = HexagonDimension.parse(json.getString("dimension"));
            this.score = json.getInt("score");
            this.description = json.getString("description");
            this.rate = json.getInt("rate");
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("dimension", this.hexagonDimension.name);
            json.put("score", this.score);
            json.put("description", this.description);
            json.put("rate", this.rate);
            return json;
        }
    }
}
