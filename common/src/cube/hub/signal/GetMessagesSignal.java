/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
