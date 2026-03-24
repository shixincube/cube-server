/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import cube.common.JSONable;
import cube.common.entity.User;
import org.json.JSONObject;

/**
 * 报告数据关系描述。
 */
public class ConversationRelation implements JSONable {

    public String name;

    public String speechFileCode;

    public long reportSn = 0;

    public long uid = 0;

    public User user;

    public ConversationRelation() {
        this.name = "Anonymous";
    }

    public ConversationRelation(String name, long reportSn) {
        this.name = name;
        this.reportSn = reportSn;
    }

    public ConversationRelation(JSONObject json) {
        this.name = json.getString("name");
        if (json.has("speechFileCode")) {
            this.speechFileCode = json.getString("speechFileCode");
        }
        if (json.has("reportSn")) {
            this.reportSn = json.getLong("reportSn");
        }
        if (json.has("uid")) {
            this.uid = json.getLong("uid");
        }
    }

    public boolean isValidUser() {
        if (null != this.user) {
            return this.user.isRegistered();
        }

        return false;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        if (null != this.speechFileCode) {
            json.put("speechFileCode", this.speechFileCode);
        }
        if (this.reportSn > 0) {
            json.put("reportSn", this.reportSn);
        }
        if (this.uid > 0) {
            json.put("uid", this.uid);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
