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

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 媒体约束描述。
 */
public class MediaConstraint implements JSONable {

    private final boolean video;

    private final boolean audio;

    private final JSONObject dimension;

    public MediaConstraint() {
        this.video = true;
        this.audio = true;
        this.dimension = new JSONObject();
        this.dimension.put("width", 640);
        this.dimension.put("height", 480);
        this.dimension.put("constraints", new JSONObject("{ \"width\": { \"exact\": 640 }, \"height\": { \"exact\": 480 } }"));
    }

    public MediaConstraint(JSONObject json) {
        this.video = json.getBoolean("video");
        this.audio = json.getBoolean("audio");
        this.dimension = json.getJSONObject("dimension");
    }

    public boolean videoEnabled() {
        return this.video;
    }

    public boolean audioEnabled() {
        return this.audio;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("video", this.video);
        json.put("audio", this.audio);
        json.put("dimension", this.dimension);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
