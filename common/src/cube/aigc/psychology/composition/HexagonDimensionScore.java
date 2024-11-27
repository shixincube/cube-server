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

package cube.aigc.psychology.composition;

import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.algorithm.PaintingConfidence;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HexagonDimensionScore {

    private Map<HexagonDimension, Integer> scores = new LinkedHashMap<>();

    private Map<HexagonDimension, String> descriptions = new LinkedHashMap<>();

    private Map<HexagonDimension, Integer> rates = new LinkedHashMap<>();

    public HexagonDimensionScore() {
    }

    public HexagonDimensionScore(int mood, int cognition, int behavior, int interpersonalRelationship,
                                 int selfAssessment, int mentalHealth) {
        this.record(HexagonDimension.Mood, mood);
        this.record(HexagonDimension.Cognition, cognition);
        this.record(HexagonDimension.Behavior, behavior);
        this.record(HexagonDimension.InterpersonalRelationship, interpersonalRelationship);
        this.record(HexagonDimension.SelfAssessment, selfAssessment);
        this.record(HexagonDimension.MentalHealth, mentalHealth);
    }

    public HexagonDimensionScore(List<EvaluationScore> scoreList, PaintingConfidence confidence,
                                 FactorSet factorSet) {
        HexagonDimensionScore candidate = Resource.getInstance().getHexDimProjection().calc(scoreList);
        for (HexagonDimension hd : HexagonDimension.values()) {
            this.record(hd, candidate.getDimensionScore(hd));
        }

        if (null != confidence) {
            // 认知
            switch (confidence.getConfidenceLevel()) {
                case PaintingConfidence.LEVEL_HIGHER:
                    this.record(HexagonDimension.Cognition,
                            Utils.randomInt(candidate.getDimensionScore(HexagonDimension.Cognition), 99));
                    break;
                case PaintingConfidence.LEVEL_HIGH:
                    this.record(HexagonDimension.Cognition, Utils.randomInt(80, 90));
                    break;
                case PaintingConfidence.LEVEL_NORMAL:
                    this.record(HexagonDimension.Cognition, Utils.randomInt(70, 80));
                    break;
                case PaintingConfidence.LEVEL_LOW:
                    this.record(HexagonDimension.Cognition, Utils.randomInt(60, 70));
                    break;
                case PaintingConfidence.LEVEL_LOWER:
                    this.record(HexagonDimension.Cognition, Utils.randomInt(50, 60));
                    break;
                default:
                    break;
            }
        }

        if (null != factorSet) {
            // 情绪
            if (factorSet.affectFactor.positive > factorSet.affectFactor.negative) {
                if (factorSet.affectFactor.positive - factorSet.affectFactor.negative > 10) {
                    this.record(HexagonDimension.Mood, Utils.randomInt(80, 90));
                }
                else {
                    this.record(HexagonDimension.Mood, Utils.randomInt(70, 80));
                }
            }
            else {
                if (factorSet.affectFactor.negative - factorSet.affectFactor.positive > 10) {
                    this.record(HexagonDimension.Mood, Utils.randomInt(50, 60));
                }
                else {
                    this.record(HexagonDimension.Mood, Utils.randomInt(60, 70));
                }
            }

            // 心理健康
            if (factorSet.calcSymptomTotal() > 160) {
                this.record(HexagonDimension.MentalHealth, Utils.randomInt(50, 59));
            }
            else {
                if (this.getDimensionScore(HexagonDimension.MentalHealth) < 70) {
                    this.record(HexagonDimension.MentalHealth, Utils.randomInt(70, 80));
                }
            }

            // 人际敏感
            if (factorSet.symptomFactor.interpersonal > 2.0) {
                this.record(HexagonDimension.InterpersonalRelationship, Utils.randomInt(50, 59));
            }
            else if (factorSet.symptomFactor.interpersonal > 1.66) {
                this.record(HexagonDimension.InterpersonalRelationship, Utils.randomInt(60, 70));
            }
            else {
                this.record(HexagonDimension.InterpersonalRelationship, Utils.randomInt(80, 90));
            }
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

    public void record(HexagonDimension dim, int value) {
        this.scores.put(dim, value);
    }

    public void record(HexagonDimension dim, int rate, String description) {
        this.rates.put(dim, rate);
        this.descriptions.put(dim, description);
    }

    public int getDimensionScore(HexagonDimension hexagonDimension) {
        if (!this.scores.containsKey(hexagonDimension)) {
            return 0;
        }
        return this.scores.get(hexagonDimension);
    }

    public String getDimensionDescription(HexagonDimension hexagonDimension) {
        return this.descriptions.get(hexagonDimension);
    }

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
}
