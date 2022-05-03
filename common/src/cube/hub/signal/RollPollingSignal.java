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

import cube.common.entity.ConversationType;
import org.json.JSONObject;

/**
 * 轮询信令。
 */
public class RollPollingSignal extends Signal {

    public final static String NAME = "RollPolling";

    private ConversationType conversationType;

    private String conversationName;

    private int limit = 5;

    public RollPollingSignal(String code, ConversationType conversationType, String conversationName) {
        super(NAME);
        setCode(code);
        this.conversationType = conversationType;
        this.conversationName = conversationName;
    }

    public RollPollingSignal(JSONObject json) {
        super(json);
        this.conversationType = ConversationType.parse(json.getInt("conversationType"));
        this.conversationName = json.getString("conversationName");
        this.limit = json.getInt("limit");
    }

    public ConversationType getConversationType() {
        return this.conversationType;
    }

    public String getConversationName() {
        return this.conversationName;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getLimit() {
        return this.limit;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("conversationType", this.conversationType.code);
        json.put("conversationName", this.conversationName);
        json.put("limit", this.limit);
        return json;
    }
}
