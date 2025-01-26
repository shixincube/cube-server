/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import cube.aigc.psychology.algorithm.Representation;
import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PaintingLabel implements JSONable {

    public final static int STATE_NORMAL = 0;

    public final static int STATE_IGNORE = 1;

    private long sn;

    private long timestamp;

    private String description;

    private List<EvaluationScore> evaluationScores;

    private List<Representation> representations;

    public PaintingLabel(JSONObject json) {
        this.sn = json.getLong("sn");
        this.timestamp = json.getLong("timestamp");
        this.description = json.getString("description");
        if (json.has("evaluationScores")) {
            this.evaluationScores = new ArrayList<>();
            JSONArray array = json.getJSONArray("evaluationScores");
            for (int i = 0; i < array.length(); ++i) {
                this.evaluationScores.add(new EvaluationScore(array.getJSONObject(i)));
            }
        }
        if (json.has("representations")) {
            this.representations = new ArrayList<>();
            JSONArray array = json.getJSONArray("representations");
            for (int i = 0; i < array.length(); ++i) {
                this.representations.add(new Representation(array.getJSONObject(i)));
            }
        }
    }

    public PaintingLabel(long sn, long timestamp, String description, JSONArray evaluationScores) {
        this.sn = sn;
        this.timestamp = timestamp;
        this.description = description;
        this.evaluationScores = new ArrayList<>();
        for (int i = 0; i < evaluationScores.length(); ++i) {
            this.evaluationScores.add(new EvaluationScore(evaluationScores.getJSONObject(i)));
        }
    }

    public long getSn() {
        return this.sn;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public String getDescription() {
        return this.description;
    }

    public List<EvaluationScore> getEvaluationScores() {
        return this.evaluationScores;
    }

    public JSONArray getEvaluationScoresAsJSONArray() {
        JSONArray array = new JSONArray();
        for (EvaluationScore score : this.evaluationScores) {
            JSONObject scoreJson = score.toJSON();
            scoreJson.put("indicator", score.indicator.code);
            array.put(scoreJson);
        }
        return array;
    }

    public List<Representation> getRepresentations() {
        return this.representations;
    }

    public JSONArray getRepresentationsAsJSONArray() {
        if (null == this.representations) {
            return null;
        }
        JSONArray array = new JSONArray();
        for (Representation representation : this.representations) {
            array.put(representation.toJSON());
        }
        return array;
    }

    public void setRepresentations(JSONArray array) {
        this.representations = new ArrayList<>();
        for (int i = 0; i < array.length(); ++i) {
            this.representations.add(new Representation(array.getJSONObject(i)));
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = this.toCompactJSON();
        if (null != this.evaluationScores) {
            json.put("evaluationScores", this.getEvaluationScoresAsJSONArray());
        }
        if (null != this.representations) {
            json.put("representations", this.getRepresentationsAsJSONArray());
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("timestamp", this.timestamp);
        json.put("description", this.description);
        return json;
    }
}
