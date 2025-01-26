/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import cube.common.JSONable;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ScalePrompt implements JSONable {

    private List<Factor> factors;

    public ScalePrompt() {
        this.factors = new ArrayList<>();
    }

    public ScalePrompt(JSONObject json) {
        this.factors = new ArrayList<>();
        JSONArray array = json.getJSONArray("factors");
        for (int i = 0; i < array.length(); ++i) {
            this.factors.add(new Factor(array.getJSONObject(i)));
        }
    }

    public boolean isEmpty() {
        return this.factors.isEmpty();
    }

    public void addPrompt(Object value) {
        if (value instanceof ScriptObjectMirror) {
            ScriptObjectMirror som = (ScriptObjectMirror) value;
            Factor factor = new Factor();
            if (som.containsKey("factor")) {
                factor.factor = (String) som.get("factor");
            }
            if (som.containsKey("score")) {
                try {
                    factor.score = (Double) som.get("score");
                } catch (Exception e) {
                    try {
                        factor.score = (Integer) som.get("score");
                    } catch (Exception ie) {
                        factor.score = 0.0;
                    }
                }
            }
            if (som.containsKey("name")) {
                factor.name = (String) som.get("name");
            }
            if (som.containsKey("description")) {
                factor.description = (String) som.get("description");
            }
            if (som.containsKey("suggestion")) {
                factor.suggestion = (String) som.get("suggestion");
            }

            this.factors.add(factor);
        }
    }

    public List<Factor> getFactors() {
        return this.factors;
    }

    public Factor getFactor(String name) {
        for (Factor factor : this.factors) {
            if (factor.factor.equals(name) || factor.name.equals(name)) {
                return factor;
            }
        }

        return null;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        JSONArray array = new JSONArray();
        for (Factor factor : this.factors) {
            array.put(factor.toJSON());
        }
        json.put("factors", array);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    public class Factor {

        public String factor = "";

        public double score = 0;

        public String name = "";

        public String description = "";

        public String suggestion = "";

        public Factor() {
        }

        public Factor(JSONObject json) {
            this.factor = json.getString("factor");
            this.score = json.getDouble("score");
            this.name = json.getString("name");
            this.description = json.getString("description");
            this.suggestion = json.getString("suggestion");
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("factor", this.factor);
            json.put("score", this.score);
            json.put("name", this.name);
            json.put("description", this.description);
            json.put("suggestion", this.suggestion);
            return json;
        }
    }
}
