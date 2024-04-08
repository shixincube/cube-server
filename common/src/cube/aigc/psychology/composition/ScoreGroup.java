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

import cube.aigc.psychology.Indicator;
import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ScoreGroup implements JSONable {

    private List<Score> scoreList;

    private List<EvaluationScore> evaluationScoreList;

    public ScoreGroup() {
        this.scoreList = new ArrayList<>();
    }

    public ScoreGroup(JSONObject json) {
        this.scoreList = new ArrayList<>();
        if (json.has("scores")) {
            JSONArray array = json.getJSONArray("scores");
            for (int i = 0; i < array.length(); ++i) {
                this.scoreList.add(new Score(array.getJSONObject(i)));
            }
        }
        if (json.has("evaluations")) {
            this.evaluationScoreList = new ArrayList<>();
            JSONArray array = json.getJSONArray("evaluations");
            for (int i = 0; i < array.length(); ++i) {
                this.evaluationScoreList.add(new EvaluationScore(array.getJSONObject(i)));
            }
        }
    }

    public void addScore(Score score) {
        this.scoreList.add(score);
    }

    public int getOriginalValue(Indicator indicator) {
        int value = 0;
        for (Score score : this.scoreList) {
            if (score.indicator == indicator) {
                value += score.value;
            }
        }
        return value;
    }

    public List<Score> getScoresOrderByWeight() {
        List<Score> list = new ArrayList<>(this.scoreList);

        Collections.sort(list, new Comparator<Score>() {
            @Override
            public int compare(Score s1, Score s2) {
                return (int) (s2.weight - s1.weight);
            }
        });

        return list;
    }

//    public List<Score> getScoresOrderByValue() {
//        List<Score> list = new ArrayList<>();
//        // 合并得分项
//        for (Score score : this.scoreList) {
//            int index = list.indexOf(score);
//            if (index >= 0) {
//                // 合并
//                Score current = list.get(index);
//                current.value += score.value;
//                current.weight += score.weight;
//            }
//            else {
//                list.add(new Score(score.indicator, score.value, score.weight));
//            }
//        }
//        // 排序
//        Collections.sort(list, new Comparator<Score>() {
//            @Override
//            public int compare(Score s1, Score s2) {
//                return s2.value - s1.value;
//            }
//        });
//        return list;
//    }

    public List<EvaluationScore> getEvaluationScores() {
        if (null != this.evaluationScoreList) {
            return this.evaluationScoreList;
        }

        List<EvaluationScore> result = new ArrayList<>();

        // 合并得分项
        for (Score score : this.scoreList) {
            EvaluationScore es = this.findEvaluationScore(result, score.indicator);
            if (null == es) {
                result.add(new EvaluationScore(score.indicator, score.value, score.value * score. weight));
            }
            else {
                es.total += score.value;
                es.score += score.value * score.weight;
            }
        }

        // 排序
        Collections.sort(result, new Comparator<EvaluationScore>() {
            @Override
            public int compare(EvaluationScore s1, EvaluationScore s2) {
                return s2.total - s1.total;
            }
        });

        return result;
    }

    private EvaluationScore findEvaluationScore(List<EvaluationScore> list, Indicator indicator) {
        for (EvaluationScore es : list) {
            if (es.indicator == indicator) {
                return es;
            }
        }
        return null;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        JSONArray list = new JSONArray();
        for (Score score : this.scoreList) {
            list.put(score.toJSON());
        }
        json.put("scores", list);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        JSONArray list = new JSONArray();
        for (EvaluationScore score : this.getEvaluationScores()) {
            list.put(score.toJSON());
        }
        json.put("evaluations", list);
        return json;
    }
}
