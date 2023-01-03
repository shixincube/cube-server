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

package cube.hub.event;

import cube.hub.data.ChannelCode;
import org.json.JSONObject;

/**
 * 应答。
 */
public class AckEvent extends WeChatEvent {

    public final static String NAME = "Ack";

    private String ackSignal;

    public AckEvent(ChannelCode channelCode, String ackSignal) {
        super(NAME);
        setCode(channelCode.code);
        this.ackSignal = ackSignal;
    }

    public AckEvent(long sn) {
        super(sn, NAME);
    }

    public AckEvent(JSONObject json) {
        super(json);
        if (json.has("ackSignal")) {
            this.ackSignal = json.getString("ackSignal");
        }
    }

    public String getAckSignal() {
        return this.ackSignal;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        if (null != this.ackSignal) {
            json.put("ackSignal", this.ackSignal);
        }
        return json;
    }
}
