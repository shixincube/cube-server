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

import cell.util.Utils;
import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 提示词记录。
 */
public class PromptRecord implements JSONable {

    public final long id;

    public final String act;

    public final String prompt;

    public final boolean readonly;

    public PromptRecord(long id, String act, String prompt, boolean readonly) {
        this.id = id;
        this.act = act;
        this.prompt = prompt;
        this.readonly = readonly;
    }

    public PromptRecord(JSONObject json) {
        this.id = json.has("id") ? json.getLong("id") : Utils.generateSerialNumber();
        this.act = json.getString("act");
        this.prompt = json.getString("prompt");
        this.readonly = json.getBoolean("readonly");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("act", this.act);
        json.put("prompt", this.prompt);
        json.put("readonly", this.readonly);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
