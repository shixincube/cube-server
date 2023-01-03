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

package cube.hub.signal;

import cube.hub.data.ChannelCode;
import org.json.JSONObject;

/**
 * 频道码信令。
 */
public class ChannelCodeSignal extends Signal {

    public final static String NAME = "ChannelCode";

    private ChannelCode channelCode;

    public ChannelCodeSignal(String code) {
        super(NAME);
        setCode(code);
    }

    public ChannelCodeSignal(ChannelCode channelCode) {
        super(NAME);
        this.channelCode = channelCode;
    }

    public ChannelCodeSignal(JSONObject json) {
        super(json);
        if (json.has("channelCode")) {
            this.channelCode = new ChannelCode(json.getJSONObject("channelCode"));
        }
    }

    public ChannelCode getChannelCode() {
        return this.channelCode;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        if (null != this.channelCode) {
            json.put("channelCode", this.channelCode.toJSON());
        }
        return json;
    }
}
