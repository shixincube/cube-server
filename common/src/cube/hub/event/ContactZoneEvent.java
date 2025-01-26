/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.event;

import cube.common.entity.ContactZone;
import cube.hub.data.DataHelper;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 联系人 Zone 数据事件。
 */
public class ContactZoneEvent extends WeChatEvent {

    public final static String NAME = "ContactZone";

    private ContactZone contactZone;

    private int beginIndex;

    private int endIndex;

    private int totalSize;

    public ContactZoneEvent(ContactZone contactZone, int beginIndex, int endIndex, int totalSize) {
        super(NAME);
        this.contactZone = contactZone;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        this.totalSize = totalSize;
    }

    public ContactZoneEvent(JSONObject json) {
        super(json);
        this.contactZone = new ContactZone(json.getJSONObject("zone"));
        this.beginIndex = json.getInt("begin");
        this.endIndex = json.getInt("end");
        this.totalSize = json.getInt("total");
    }

    public ContactZone getContactZone() {
        return this.contactZone;
    }

    public int getBeginIndex() {
        return this.beginIndex;
    }

    public int getEndIndex() {
        return this.endIndex;
    }

    public int getTotalSize() {
        return this.totalSize;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("zone", this.contactZone.toJSON());
        json.put("begin", this.beginIndex);
        json.put("end", this.endIndex);
        json.put("total", this.totalSize);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        json.put("begin", this.beginIndex);
        json.put("end", this.endIndex);
        json.put("total", this.totalSize);

        // 过滤数据
        JSONObject zoneJson = new JSONObject(this.contactZone.toJSON().toMap());
        JSONArray participants = zoneJson.getJSONArray("participants");
        for (int i = 0; i < participants.length(); ++i) {
            JSONObject part = participants.getJSONObject(i);
            if (part.has("linkedContact")) {
                part.put("linkedContact",
                        DataHelper.filterContactAvatarFileLabel(part.getJSONObject("linkedContact")));
            }
        }

        json.put("zone", zoneJson);
        return json;
    }
}
