/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology;

import cube.aigc.psychology.algorithm.IndicatorRate;
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
 * @deprecated
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
            result.recordScore(dim, (int) Math.ceil(value), IndicatorRate.None);
            ++index;
        }

        return result;
    }

    public class Projection {

        public HexagonDimension hexagonDimension;

        public Map<Indicator, Double> weights;

        public Projection(HexagonDimension hexagonDimension) {
            this.hexagonDimension = hexagonDimension;
            this.weights = new LinkedHashMap<>();
        }
    }
}
