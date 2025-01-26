/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.event;

import cube.common.entity.Contact;
import cube.common.entity.Group;
import cube.hub.data.DataHelper;
import org.json.JSONObject;

/**
 * 群组数据事件。
 */
public class GroupDataEvent extends WeChatEvent {

    public final static String NAME = "GroupData";

    private Group group;

    public GroupDataEvent(Contact account, Group group) {
        super(NAME, account);
        this.group = group;
    }

    public GroupDataEvent(JSONObject json) {
        super(json);
        this.group = new Group(json.getJSONObject("group"));
    }

    public Group getGroup() {
        return this.group;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("group", this.group.toJSON());
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        json.put("group", DataHelper.filterContactAvatarFileLabel(this.group.toJSON()));
        return json;
    }
}
