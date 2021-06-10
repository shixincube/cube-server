/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

package cube.service.multipointcomm;

import cube.common.JSONable;
import cube.common.entity.CommField;
import cube.common.entity.CommFieldEndpoint;
import cube.service.multipointcomm.event.MicrophoneVolume;
import org.json.JSONObject;

/**
 * 广播数据。
 */
public class Broadcast implements JSONable {

    private CommField commField;

    private CommFieldEndpoint source;

    private CommFieldEndpoint target;

    private JSONObject data;

    public Broadcast(CommField commField, CommFieldEndpoint source, CommFieldEndpoint target, JSONObject data) {
        this.commField = commField;
        this.source = source;
        this.target = target;
        this.data = data;
    }

    public Broadcast(JSONObject json) {
        this.commField = new CommField(json.getJSONObject("field"));
        this.source = new CommFieldEndpoint(json.getJSONObject("source"));
        this.target = new CommFieldEndpoint(json.getJSONObject("target"));
        this.data = json.getJSONObject("data");
    }

    public CommFieldEndpoint getSource() {
        return this.source;
    }

    public CommFieldEndpoint getTarget() {
        return this.target;
    }

    public boolean isMicrophoneVolume() {
        if (!this.data.has("event")) {
            return false;
        }

        return this.data.getString("event").equals(MicrophoneVolume.NAME);
    }

    public MicrophoneVolume extractMicrophoneVolume() {
        if (this.isMicrophoneVolume()) {
            return new MicrophoneVolume(this.data);
        }

        return null;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("field", this.commField.toCompactJSON());
        json.put("source", this.source.toJSON());
        json.put("target", this.target.toJSON());
        json.put("data", this.data);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return toJSON();
    }


    public static boolean isMicrophoneVolume(JSONObject data) {
        return false;
    }
}
