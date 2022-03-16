/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

package cube.hub;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 消息。
 */
public class MetaMessage implements JSONable {

    private long id;

    private JSONObject meta;

    private String sender;

    private String ground;

    private long timestamp;

    public MetaMessage(long id, String sender, String ground, long timestamp,
                       JSONObject meta) {
        this.id = id;
        this.sender = sender;
        this.ground = ground;
        this.timestamp = timestamp;
        this.meta = meta;
    }

    public MetaMessage(JSONObject json) {
        this.id = json.getLong("id");
        this.sender = json.getString("sender");
        this.ground = json.getString("ground");
        this.timestamp = json.getLong("timestamp");
        this.meta = json.getJSONObject("meta");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("sender", this.sender);
        json.put("ground", this.ground);
        json.put("timestamp", this.timestamp);
        json.put("meta", this.meta);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
