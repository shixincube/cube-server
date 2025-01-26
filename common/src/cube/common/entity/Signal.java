/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cell.util.Utils;
import org.json.JSONObject;

/**
 * 信号。
 */
public class Signal extends Entity {

    private Contact destContact;

    private Device destDevice;

    private Contact srcContact;

    private Device srcDevice;

    private JSONObject payload;

    public Signal() {
        super(Utils.generateSerialNumber());
    }

    public Signal(JSONObject json) {
        super(json);

        if (json.has("destContact")) {
            this.destContact = new Contact(json.getJSONObject("destContact"));
        }
        if (json.has("destDevice")) {
            this.destDevice = new Device(json.getJSONObject("destDevice"));
        }
        if (json.has("srcContact")) {
            this.srcContact = new Contact(json.getJSONObject("srcContact"));
        }
        if (json.has("srcDevice")) {
            this.srcDevice = new Device(json.getJSONObject("srcDevice"));
        }

        if (json.has("payload")) {
            this.payload = json.getJSONObject("payload");
        }
    }

    public Contact getDestContact() {
        return this.destContact;
    }

    public void setDestContact(Contact contact) {
        this.destContact = contact;
    }

    public Device getDestDevice() {
        return this.destDevice;
    }

    public Contact getSrcContact() {
        return this.srcContact;
    }

    public Device getSrcDevice() {
        return this.srcDevice;
    }

    public JSONObject getPayload() {
        return this.payload;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id.longValue());

        if (null != this.destContact) {
            json.put("destContact", this.destContact.toBasicJSON());
        }

        if (null != this.destDevice) {
            json.put("destDevice", this.destDevice.toCompactJSON());
        }

        if (null != this.srcContact) {
            json.put("srcContact", this.srcContact.toBasicJSON());
        }

        if (null != this.srcDevice) {
            json.put("srcDevice", this.srcDevice.toCompactJSON());
        }

        if (null != this.payload) {
            json.put("payload", this.payload);
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = this.toJSON();
        json.remove("payload");
        return json;
    }
}
