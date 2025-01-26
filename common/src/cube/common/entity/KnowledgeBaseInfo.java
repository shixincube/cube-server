/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 知识库信息。
 */
public class KnowledgeBaseInfo implements JSONable {

    public long contactId;

    public String name;

    public String displayName;

    public String category;

    public long unitId;

    public long storeSize;

    public long timestamp;

    public KnowledgeBaseInfo(long contactId, String name, String displayName, String category,
                             long unitId, long storeSize, long timestamp) {
        this.contactId = contactId;
        this.name = name;
        this.displayName = displayName;
        this.category = category;
        this.unitId = unitId;
        this.storeSize = storeSize;
        this.timestamp = timestamp;
    }

    public KnowledgeBaseInfo(JSONObject json) {
        this.contactId = json.getLong("contactId");
        this.name = json.getString("name");
        this.displayName = json.getString("displayName");
        this.unitId = json.getLong("unitId");
        this.storeSize = json.getLong("storeSize");
        this.timestamp = json.getLong("timestamp");
        if (json.has("category") && json.getString("category").trim().length() > 0) {
            this.category = json.getString("category");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof KnowledgeBaseInfo) {
            KnowledgeBaseInfo other = (KnowledgeBaseInfo) obj;
            return other.contactId == this.contactId && other.name.equals(this.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (new Long(this.contactId)).hashCode() * 7 - this.name.hashCode();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("contactId", this.contactId);
        json.put("name", this.name);
        json.put("displayName", this.displayName);
        json.put("unitId", this.unitId);
        json.put("storeSize", this.storeSize);
        json.put("timestamp", this.timestamp);
        if (null != this.category && this.category.length() > 0) {
            json.put("category", this.category);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
