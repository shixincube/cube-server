/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology;

import cube.aigc.psychology.composition.EvaluationScore;
import cube.aigc.psychology.composition.HexagonDimension;
import cube.aigc.psychology.composition.HexagonDimensionScore;
import cube.util.FloatUtils;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class HexagonDimensionProjection {

    private Map<HexagonDimension, Projection> projections;

    public HexagonDimensionProjection(JSONObject json) {
        this.projections = new LinkedHashMap<>();

        Iterator<String> iter = json.keys();
        while (iter.hasNext()) {
            String key = iter.next();

            HexagonDimension hexagonDimension = HexagonDimension.parse(key);
            Projection projection = new Projection(hexagonDimension);

            JSONObject value = json.getJSONObject(key);
            Iterator<String> indicatorIter = value.keys();
            while (indicatorIter.hasNext()) {
                String indicatorKey = indicatorIter.next();
                double weight = value.getDouble(indicatorKey);

                Indicator indicator = Indicator.parse(indicatorKey);
                projection.weights.put(indicator, weight);
            }

            this.projections.put(hexagonDimension, projection);
        }
    }

    public synchronized HexagonDimensionScore calc(List<EvaluationScore> scoreList) {
        HexagonDimensionScore result = new HexagonDimensionScore();

        double[] scores = new double[scoreList.size()];
        for (int i = 0; i < scores.length; ++i) {
            EvaluationScore es = scoreList.get(i);
            scores[i] = es.calcScore();
        }

        scores = FloatUtils.softmax(scores);

        double[] hdScores = new double[HexagonDimension.values().length];

        int index = 0;
        for (HexagonDimension dim : HexagonDimension.values()) {
            Projection projection = this.projections.get(dim);

            double sum = 0;
            for (int i = 0; i < scoreList.size(); ++i) {
                EvaluationScore es = scoreList.get(i);
                Double weight = projection.weights.get(es.indicator);
                if (null == weight) {
                    continue;
                }
                sum += scores[i] * 100 * weight;
            }

            hdScores[index] = sum;
            ++index;
        }

        hdScores = FloatUtils.normalization(hdScores, 60, 99);
        index = 0;
        for (HexagonDimension dim : HexagonDimension.values()) {
            double value = hdScores[index];
            result.record(dim, (int) Math.ceil(value));
            ++index;
        }

        /*HexagonDimension[] hexagonDimensions = new HexagonDimension[6];
        double[] values = new double[6];
        int index = 0;
        double totalFive = 0;

        for (Map.Entry<HexagonDimension, Projection> e : this.projections.entrySet()) {
            HexagonDimension dim = e.getKey();

            Projection projection = e.getValue();
            double sum = 0;
            for (Map.Entry<Indicator, Double> weight : projection.weights.entrySet()) {
                EvaluationScore score = getScore(weight.getKey(), scoreList);
                if (null == score) {
                    continue;
                }

                double delta = score.calcScore();
//                if (delta > 0) {
//                    sum += delta * 10d * weight.getValue();
//                }
                sum += delta * weight.getValue() * 10d;
            }

            hexagonDimensions[index] = dim;
            values[index++] = sum;
            if (dim != HexagonDimension.MentalHealth) {
                totalFive += sum;
            }
        }

        for (int i = 0; i < values.length; ++i) {
            if (hexagonDimensions[i] == HexagonDimension.MentalHealth && values[i] < 10) {
                values[i] = totalFive / 5.0;
            }

            // 当分数较低时调整到低分下限
            int value = Math.min(Math.max(1, (int) Math.round(values[i])), 99);
            while (value <= 20) {
                value += value;
            }

            result.record(hexagonDimensions[i], value);
        }*/

        return result;
    }

//    private EvaluationScore getScore(Indicator indicator, List<EvaluationScore> scoreList) {
//        for (EvaluationScore score : scoreList) {
//            if (score.indicator == indicator) {
//                return score;
//            }
//        }
//
//        return null;
//    }


    public class Projection {

        public HexagonDimension hexagonDimension;

        public Map<Indicator, Double> weights;

        public Projection(HexagonDimension hexagonDimension) {
            this.hexagonDimension = hexagonDimension;
            this.weights = new LinkedHashMap<>();
        }
    }
}
