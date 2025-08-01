/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.algorithm;

import cell.util.log.Logger;
import cube.aigc.psychology.Indicator;
import cube.aigc.psychology.composition.EvaluationScore;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * @deprecated
 */
public class Benchmark {

    private List<GenerationBenchmark> benchmarkList;

    public Benchmark(JSONArray array) {
        this.benchmarkList = new ArrayList<>();
        for (int i = 0; i < array.length(); ++i) {
            JSONObject json = array.getJSONObject(i);
            GenerationBenchmark benchmark = new GenerationBenchmark(json);
            this.benchmarkList.add(benchmark);
        }
    }

    public List<EvaluationScore> getEvaluationScores(int age) {
        List<EvaluationScore> list = new ArrayList<>();
        GenerationBenchmark benchmark = this.getGenerationBenchmark(age);
        if (null != benchmark) {
            for (Map.Entry<Indicator, BenchmarkScore> e : benchmark.scoreMap.entrySet()) {
                Indicator indicator = e.getKey();
                BenchmarkScore benchmarkScore = e.getValue();
                EvaluationScore score = new EvaluationScore(indicator);
                score.positiveScore = benchmarkScore.positiveMax;
                score.negativeScore = benchmarkScore.negativeMax;;
                list.add(score);
            }
        }
        return list;
    }

    private GenerationBenchmark getGenerationBenchmark(int age) {
        for (GenerationBenchmark benchmark : this.benchmarkList) {
            if (age >= benchmark.min && age <= benchmark.max) {
                return benchmark;
            }
        }
        return null;
    }

    public JSONObject toJSON(int age) {
        for (GenerationBenchmark benchmark : this.benchmarkList) {
            if (age >= benchmark.min && age <= benchmark.max) {
                JSONObject json = new JSONObject();
                for (Map.Entry<Indicator, BenchmarkScore> entry : benchmark.scoreMap.entrySet()) {
                    json.put(entry.getKey().code, entry.getValue().toJSON(entry.getKey().name));
                }
                return json;
            }
        }

        return null;
    }


    public class GenerationBenchmark {

        public int min;

        public int max;

        public Map<Indicator, BenchmarkScore> scoreMap;

        public GenerationBenchmark(JSONObject json) {
            this.scoreMap = new HashMap<>();

            JSONArray generation = json.getJSONArray("generation");
            this.min = generation.getInt(0);
            this.max = generation.getInt(1);

            JSONObject benchmark = json.getJSONObject("benchmark");
            Iterator<String> keys = benchmark.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Indicator indicator = Indicator.parse(key);
                if (null == indicator) {
                    Logger.e(this.getClass(), "Can not find indicator: " + key);
                    continue;
                }

                JSONObject value = benchmark.getJSONObject(key);
                BenchmarkScore score = new BenchmarkScore(value);
                this.scoreMap.put(indicator, score);
            }
        }
    }

    public class BenchmarkScore {

        public double positiveMin;

        public double positiveMax;

        public double negativeMin;

        public double negativeMax;

        public BenchmarkScore(JSONObject json) {
            JSONObject positive = json.getJSONObject("positive");
            JSONObject negative = json.getJSONObject("negative");
            this.positiveMin = positive.getDouble("min");
            this.positiveMax = positive.getDouble("max");
            this.negativeMin = negative.getDouble("min");
            this.negativeMax = negative.getDouble("max");
        }

        public JSONObject toJSON(String name) {
            JSONObject json = new JSONObject();
            json.put("name", name);

            JSONObject positive = new JSONObject();
            positive.put("min", this.positiveMin);
            positive.put("max", this.positiveMax);
            json.put("positive", positive);

            JSONObject negative = new JSONObject();
            negative.put("min", this.negativeMin);
            negative.put("max", this.negativeMax);
            json.put("negative", negative);

            return json;
        }
    }
}
