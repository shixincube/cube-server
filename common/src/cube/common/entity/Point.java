/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import org.json.JSONObject;

public class Point extends Entity {

    private long contactId;

    private int point;

    private String source;

    private String comment;

    public Point(String domain, long timestamp, long contactId, int point, String source, String comment) {
        super(0L, domain, timestamp);
        this.contactId = contactId;
        this.point = point;
        this.source = source;
        this.comment = comment;
    }

    public Point(long id, String domain, long timestamp, long contactId, int point, String source, String comment) {
        super(id, domain, timestamp);
        this.contactId = contactId;
        this.point = point;
        this.source = source;
        this.comment = comment;
    }

    public Point(JSONObject json) {
        super(json);
        this.contactId = json.getLong("contactId");
        this.point = json.getInt("point");
        this.source = json.getString("source");
        this.comment = json.getString("comment");
    }

    public long getContactId() {
        return this.contactId;
    }

    public int getPoint() {
        return this.point;
    }

    public String getSource() {
        return this.source;
    }

    public String getComment() {
        return this.comment;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("contactId", this.contactId);
        json.put("point", this.point);
        json.put("source", this.source);
        json.put("comment", this.comment);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
