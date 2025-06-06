/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import org.json.JSONObject;

/**
 * 会员信息。
 * 会员ID就是联系人ID。
 */
public class Membership extends Entity {

    /**
     * 普通会员。
     */
    public final static String TYPE_ORDINARY = "ordinary";

    /**
     * 资深会员。
     */
    public final static String TYPE_SENIOR = "senior";

    /**
     * 高级会员。
     */
    public final static String TYPE_PREMIUM = "premium";

    /**
     * 至尊会员。
     */
    public final static String TYPE_SUPREME = "supreme";

    /**
     * 正常状态。
     */
    public final static int STATE_NORMAL = 0;

    /**
     * 无效状态。
     */
    public final static int STATE_INVALID = 1;

    /**
     * 禁用状态。
     */
    public final static int STATE_FORBIDDEN = 9;

    public String name;

    public String type;

    public int state;

    public long duration;

    public String description;

    public JSONObject context;

    public Membership(long contactId, String domain, String name, String type, int state,
                      long timestamp, long duration, String description, JSONObject context) {
        super(contactId, domain, timestamp);
        this.name = name;
        this.type = type;
        this.state = state;
        this.duration = duration;
        this.description = description;
        this.context = context;
    }

    public Membership(JSONObject json) {
        super(json);
        this.name = json.getString("name");
        this.type = json.getString("type");
        this.state = json.getInt("state");
        this.duration = json.getLong("duration");
        this.description = json.getString("description");
        if (json.has("context")) {
            this.context = json.getJSONObject("context");
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("name", this.name);
        json.put("type", this.type);
        json.put("state", this.state);
        json.put("duration", this.duration);
        json.put("description", this.description);
        if (null != this.context) {
            json.put("context", this.context);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
