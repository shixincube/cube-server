/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm.signaling;

import cube.common.JSONable;
import cube.common.entity.CommField;
import cube.common.entity.CommFieldEndpoint;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import org.json.JSONObject;

/**
 * 信令。
 */
public abstract class Signaling implements JSONable {

    /**
     * 信令序号。
     */
    protected Long sn;

    /**
     * 信令名。
     */
    protected final String name;

    /**
     * 信令的场域。
     */
    protected CommField field;

    /**
     * 传送/接收信令的联系人。
     */
    protected Contact contact;

    /**
     * 传送/接收信令的设备。
     */
    protected Device device;

    /**
     * 目标。
     */
    protected CommFieldEndpoint target;

    /**
     * 构造函数。
     *
     * @param name
     * @param field
     * @param contact
     * @param device
     */
    public Signaling(String name, CommField field, Contact contact, Device device) {
        this(0L, name, field, contact, device);
    }

    /**
     * 构造函数。
     *
     * @param sn
     * @param name
     * @param field
     * @param contact
     * @param device
     */
    public Signaling(Long sn, String name, CommField field, Contact contact, Device device) {
        this.sn = sn;
        this.name = name;
        this.field = field;
        this.contact = contact;
        this.device = device;
    }

    /**
     * 构造函数。
     *
     * @param json
     */
    public Signaling(JSONObject json) {
        this.sn = json.getLong("sn");
        this.name = json.getString("name");
        this.field = new CommField(json.getJSONObject("field"));
        this.contact = new Contact(json.getJSONObject("contact"));
        this.device = new Device(json.getJSONObject("device"));

        if (json.has("target")) {
            this.target = new CommFieldEndpoint(json.getJSONObject("target"));
        }
    }

    public Long getSN() {
        return this.sn;
    }

    public void setSN(Long sn) {
        this.sn = sn;
    }

    public String getName() {
        return this.name;
    }

    public void setField(CommField commField) {
        this.field = commField;
    }

    public CommField getField() {
        return this.field;
    }

    public Contact getContact() {
        return this.contact;
    }

    public Device getDevice() {
        return this.device;
    }

    public CommFieldEndpoint getTarget() {
        return this.target;
    }

    public void setTarget(CommFieldEndpoint endpoint) {
        this.target = endpoint;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn.longValue());
        json.put("name", this.name);
        json.put("field", this.field.toJSON());
        json.put("contact", this.contact.toBasicJSON());
        json.put("device", this.device.toCompactJSON());

        if (null != this.target) {
            json.put("target", this.target.toCompactJSON());
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
