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

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 语音识别结果。
 */
public class ASRResult implements JSONable {

    private JSONObject data;

    public ASRResult(JSONObject json) {
        this.data = json;
    }

    public ASRResult(FileLabel fileLabel, JSONObject json) {
        this.data = json;

        this.data.put("fileCode", fileLabel.getFileCode());
        this.data.put("fileLabel", fileLabel.toCompactJSON());

        if (this.data.has("list")) {
            JSONArray list = this.data.getJSONArray("list");
            for (int i = 0; i < list.length(); ++i) {
                JSONObject item = list.getJSONObject(i);
                if (item.has("start_timestamps")) {
                    item.remove("start_timestamps");
                }
                if (item.has("end_timestamps")) {
                    item.remove("end_timestamps");
                }
            }
        }
    }

    public String getFileCode() {
        return this.data.getString("fileCode");
    }

    @Override
    public JSONObject toJSON() {
        return this.data;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
