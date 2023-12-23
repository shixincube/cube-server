/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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
 * 用量。
 */
public class Usage implements JSONable {

    public final String model;

    public final long completionTokens;

    public final long promptTokens;

    public final long totalTokens;

    public Usage(String model, long completionTokens, long promptTokens, long totalTokens) {
        this.model = model;
        this.completionTokens = completionTokens;
        this.promptTokens = promptTokens;
        this.totalTokens = totalTokens;
    }

    public Usage(JSONObject json) {
        this.model = json.has("model") ? json.getString("model") : "Baize";
        this.completionTokens = json.has("completionTokens") ?
                json.getLong("completionTokens") : json.getLong("completion_tokens");
        this.promptTokens = json.has("promptTokens") ?
                json.getLong("promptTokens") : json.getLong("prompt_tokens");
        this.totalTokens = json.has("totalTokens") ?
                json.getLong("totalTokens") : json.getLong("total_tokens");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("model", this.model);
        json.put("completionTokens", this.completionTokens);
        json.put("completion_tokens", this.completionTokens);
        json.put("promptTokens", this.promptTokens);
        json.put("prompt_tokens", this.promptTokens);
        json.put("totalTokens", this.totalTokens);
        json.put("total_tokens", this.totalTokens);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
