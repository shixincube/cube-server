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

package cube.aigc;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 提示词。
 */
public class Prompt implements JSONable {

    public String role;

    public String query;

    public String answer;

    public Prompt(String query) {
        this.query = query;
    }

    public Prompt(String query, String answer) {
        this.query = query;
        this.answer = answer;
    }

    public Prompt(JSONObject json) {
        if (json.has("role")) {
            this.role = json.getString("role");
        }
        if (json.has("query")) {
            this.query = json.getString("query");
        }
        if (json.has("answer")) {
            this.answer = json.getString("answer");
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        if (null != this.role) {
            json.put("role", this.role);
        }
        if (null != this.query) {
            json.put("query", this.query);
        }
        if (null != this.answer) {
            json.put("answer", this.answer);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
