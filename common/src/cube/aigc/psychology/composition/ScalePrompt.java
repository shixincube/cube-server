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

    public void addPrompt(Object value) {
        if (value instanceof ScriptObjectMirror) {
            ScriptObjectMirror som = (ScriptObjectMirror) value;
            Factor factor = new Factor();
            if (som.containsKey("factor"))
                factor.factor = (String) som.get("factor");
            if (som.containsKey("score"))
                factor.score = (Double) som.get("score");
            if (som.containsKey("name"))
                factor.name = (String) som.get("name");
            if (som.containsKey("description"))
                factor.description = (String) som.get("description");
            if (som.containsKey("suggestion"))
                factor.suggestion = (String) som.get("suggestion");

            this.factors.add(factor);
        }
    }

    public List<Factor> getFactors() {
        return this.factors;
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
