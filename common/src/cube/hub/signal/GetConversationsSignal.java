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

package cube.hub.signal;

import org.json.JSONObject;

/**
 * 获取消息信令。
 */
public class GetConversationsSignal extends Signal {

    public final static String NAME = "GetConversations";

    private int numConversations = 10;

    private int numRecentMessages = 5;

    public GetConversationsSignal(String channelCode) {
        super(NAME);
        setCode(channelCode);
    }

    public GetConversationsSignal(JSONObject json) {
        super(json);
        if (json.has("numConversations")) {
            this.numConversations = json.getInt("numConversations");
        }
        if (json.has("numRecentMessages")) {
            this.numRecentMessages = json.getInt("numRecentMessages");
        }
    }

    public int getNumConversations() {
        return this.numConversations;
    }

    public void setNumConversations(int value) {
        this.numConversations = value;
    }

    public int getNumRecentMessages() {
        return this.numRecentMessages;
    }

    public void setNumRecentMessages(int value) {
        this.numRecentMessages = value;
    }

    @Override
    public JSONObject toJSON() {
         JSONObject json = super.toJSON();
         json.put("numConversations", this.numConversations);
         json.put("numRecentMessages", this.numRecentMessages);
         return json;
    }
}