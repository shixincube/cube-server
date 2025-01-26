/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.signal;

import org.json.JSONObject;

/**
 * 获取群组数据信令。
 */
public class GetGroupSignal extends Signal {

    public final static String NAME = "GetGroup";

    private String groupName;

    public GetGroupSignal(String channelCode, String groupName) {
        super(NAME);
        setCode(channelCode);
        this.groupName = groupName;
    }

    public GetGroupSignal(JSONObject json) {
        super(json);
        this.groupName = json.getString("groupName");
    }

    public String getGroupName() {
        return this.groupName;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("groupName", this.groupName);
        return json;
    }
}
