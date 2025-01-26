/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.contact;

import cube.common.JSONable;
import cube.common.entity.Group;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 群组操作时受影响的相关数据描述。
 */
public class GroupBundle implements JSONable {

    public Group group;

    public List<Long> members;

    public Long operatorId;

    public GroupBundle(Group group, Long member) {
        this.group = group;
        this.members = new ArrayList<>();
        this.members.add(member);
    }

    public GroupBundle(Group group, List<Long> members) {
        this.group = group;
        this.members = new ArrayList<>(members);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            // group
            json.put("group", this.group.toJSON());

            // members
            JSONArray memberList = new JSONArray();
            for (Long memberId : this.members) {
                memberList.put(memberId);
            }
            json.put("modified", memberList);

            // operator
            if (null != this.operatorId) {
                json.put("operator", this.operatorId.longValue());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }

    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        try {
            // group
            json.put("group", this.group.toCompactJSON());

            // members
            JSONArray memberList = new JSONArray();
            for (Long memberId : this.members) {
                memberList.put(memberId);
            }
            json.put("modified", memberList);

            // operator
            if (null != this.operatorId) {
                json.put("operator", this.operatorId.longValue());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }
}
