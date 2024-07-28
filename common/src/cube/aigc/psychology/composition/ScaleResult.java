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
import org.json.JSONObject;

public class ScaleResult implements JSONable {

    public String content;

    public ScaleScore score;

    public ScalePrompt prompt;

    public boolean complete;

    public ScaleResult(Scale scale) {
        this.complete = scale.isComplete();
    }

    public ScaleResult(String content, ScaleScore score, ScalePrompt prompt, Scale scale) {
        this.content = content;
        this.score = score;
        this.prompt = prompt;
        this.complete = scale.isComplete();
    }

    public ScaleResult(JSONObject json) {
        this.complete = json.getBoolean("complete");
        if (json.has("content")) {
            this.content = json.getString("content");
        }
        if (json.has("score")) {
            this.score = new ScaleScore(json.getJSONObject("score"));
        }
        if (json.has("prompt")) {
            this.prompt = new ScalePrompt(json.getJSONObject("prompt"));
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = this.toCompactJSON();
        if (null != this.prompt) {
            json.put("prompt", this.prompt.toJSON());
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("complete", this.complete);
        if (null != this.content) {
            json.put("content", this.content);
        }
        if (null != this.score) {
            json.put("score", this.score.toJSON());
        }
        return json;
    }
}
