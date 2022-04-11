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
public class GetMessagesSignal extends Signal {

    public final static String NAME = "GetMessages";

    private String partnerId;

    private String groupName;

    private int beginIndex;

    private int endIndex;

    public GetMessagesSignal(String channelCode, int beginIndex, int endIndex) {
        super(NAME);
        setCode(channelCode);
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
    }

    public GetMessagesSignal(JSONObject json) {
        super(json);
        if (json.has("groupName")) {
            this.groupName = json.getString("groupName");
        }
        if (json.has("partnerId")) {
            this.partnerId = json.getString("partnerId");
        }

        if (json.has("begin")) {
            this.beginIndex = json.getInt("begin");
        }

        if (json.has("end")) {
            this.endIndex = json.getInt("end");
        }
    }

    public int getBeginIndex() {
        return this.beginIndex;
    }

    public int getEndIndex() {
        return this.endIndex;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public String getPartnerId() {
        return this.partnerId;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupName() {
        return this.groupName;
    }

    @Override
    public JSONObject toJSON() {
         JSONObject json = super.toJSON();
         if (null != this.groupName) {
             json.put("groupName", this.groupName);
         }
         else if (null != this.partnerId) {
             json.put("partnerId", this.partnerId);
         }

         json.put("begin", this.beginIndex);
         json.put("end", this.endIndex);

         return json;
    }
}
