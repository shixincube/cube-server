/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.algorithm;

import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.Indicator;
import cube.aigc.psychology.composition.EvaluationScore;
import cube.common.JSONable;
import cube.common.Language;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ScoreAccelerator implements JSONable {

    private List<Score> scoreList;

    private List<EvaluationScore> evaluationScoreList;

    public ScoreAccelerator() {
        this.scoreList = new ArrayList<>();
    }

    public ScoreAccelerator(JSONObject json) {
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

    public int numScores() {
        return this.scoreList.size();
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

    public List<EvaluationScore> getEvaluationScores(Attribute attribute) {
        if (null != this.evaluationScoreList) {
            return this.evaluationScoreList;
        }

        if (null == attribute) {
            attribute = new Attribute("male", 18, Language.Chinese, false);
        }

        List<EvaluationScore> result = new ArrayList<>();

        // 合并得分项
        for (Score score : this.scoreList) {
            EvaluationScore es = this.findEvaluationScore(result, score.indicator);
            if (null == es) {
                result.add(new EvaluationScore(score.indicator, score.value, score.weight, attribute));
            }
            else {
                es.scoring(score);
            }
        }

        // 排序
        Collections.sort(result, new Comparator<EvaluationScore>() {
            @Override
            public int compare(EvaluationScore s1, EvaluationScore s2) {
                return s2.indicator.priority - s1.indicator.priority;
            }
        });

        this.evaluationScoreList = new ArrayList<>();
        this.evaluationScoreList.addAll(result);

        return result;
    }

    public EvaluationScore getEvaluationScore(Indicator indicator, Attribute attribute) {
        List<EvaluationScore> list = this.getEvaluationScores(attribute);
        for (EvaluationScore es : list) {
            if (es.indicator == indicator) {
                return es;
            }
        }
        return null;
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
        for (EvaluationScore score : this.getEvaluationScores(null)) {
            list.put(score.toJSON());
        }
        json.put("evaluations", list);
        return json;
    }
}
