/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc;

import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 联系人偏好设置。
 */
public class ContactPreference implements JSONable {

    private long contactId;

    private JSONArray models;

    public ContactPreference(long contactId, JSONArray models) {
        this.contactId = contactId;
        this.models = models;
    }

    public ContactPreference(JSONObject json) {
        this.contactId = json.getLong("contactId");
        this.models = json.getJSONArray("models");
    }

    public JSONArray getModels() {
        return this.models;
    }

    public boolean containsModel(String modelName) {
        for (int i = 0; i < this.models.length(); ++i) {
            String name = this.models.getString(i);
            if (name.equalsIgnoreCase(modelName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("contactId", this.contactId);
        json.put("models", this.models);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
