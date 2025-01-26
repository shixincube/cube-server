/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 目标上下文。
 */
public class TargetContext implements JSONable {

    private Contact contact;

    private Device device;

    public TargetContext(Contact contact, Device device) {
        this.contact = contact;
        this.device = device;
    }

    public TargetContext(JSONObject json) {
        try {
            this.contact = new Contact(json.getJSONObject("contact"));
            this.device = new Device(json.getJSONObject("device"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Contact getContact() {
        return this.contact;
    }

    public Device getDevice() {
        return this.device;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("contact", this.contact.toBasicJSON());
            json.put("device", this.device.toCompactJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
