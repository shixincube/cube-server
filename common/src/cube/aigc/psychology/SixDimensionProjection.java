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

import cube.aigc.psychology.composition.EvaluationScore;
import cube.aigc.psychology.composition.SixDimension;
import cube.aigc.psychology.composition.SixDimensionScore;
import cube.util.FloatUtils;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SixDimensionProjection {

    private Map<SixDimension, Projection> projections;

    public SixDimensionProjection(JSONObject json) {
        this.projections = new LinkedHashMap<>();

        Iterator<String> iter = json.keys();
        while (iter.hasNext()) {
            String key = iter.next();

            SixDimension sixDimension = SixDimension.parse(key);
            Projection projection = new Projection(sixDimension);

            JSONObject value = json.getJSONObject(key);
            Iterator<String> indicatorIter = value.keys();
            while (indicatorIter.hasNext()) {
                String indicatorKey = indicatorIter.next();
                double weight = value.getDouble(indicatorKey);

                Indicator indicator = Indicator.parse(indicatorKey);
                projection.weights.put(indicator, weight);
            }

            this.projections.put(sixDimension, projection);
        }
    }

    public synchronized SixDimensionScore calc(List<EvaluationScore> scoreList) {
        SixDimensionScore result = new SixDimensionScore();

        SixDimension[] sixDimensions = new SixDimension[6];
        double[] values = new double[6];
        int index = 0;
        double totalFive = 0;

        for (Map.Entry<SixDimension, Projection> e : this.projections.entrySet()) {
            SixDimension dim = e.getKey();

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

            sixDimensions[index] = dim;
            values[index++] = sum;
            if (dim != SixDimension.MentalHealth) {
                totalFive += sum;
            }
        }

        for (int i = 0; i < values.length; ++i) {
            if (sixDimensions[i] == SixDimension.MentalHealth) {
                values[i] += totalFive / 5.0;
            }

            result.record(sixDimensions[i], Math.max(1, (int) Math.round(values[i])));
        }

        // 正则化
        result.normalization();

        return result;
    }

    private EvaluationScore getScore(Indicator indicator, List<EvaluationScore> scoreList) {
        for (EvaluationScore score : scoreList) {
            if (score.indicator == indicator) {
                return score;
            }
        }

        return null;
    }


    public class Projection {

        public SixDimension sixDimension;

        public Map<Indicator, Double> weights;

        public Projection(SixDimension sixDimension) {
            this.sixDimension = sixDimension;
            this.weights = new LinkedHashMap<>();
        }
    }
}
