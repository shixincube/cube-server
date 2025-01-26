/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm.signaling;

import cube.common.action.MultipointCommAction;
import cube.common.entity.CommField;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import org.json.JSONObject;

/**
 * Busy 信令。
 */
public class BusySignaling extends Signaling {

    private Contact caller;

    private Contact callee;

    public BusySignaling(CommField commField, Contact contact, Device device) {
        super(MultipointCommAction.Busy.name, commField, contact, device);
    }

    public BusySignaling(JSONObject json) {
        super(json);

        if (json.has("caller")) {
            this.caller = new Contact(json.getJSONObject("caller"));
        }
        if (json.has("callee")) {
            this.callee = new Contact(json.getJSONObject("callee"));
        }
    }

    public void copy(BusySignaling source) {
        this.caller = source.caller;
        this.callee = source.callee;
    }

    public void setCaller(Contact caller) {
        this.caller = caller;
    }

    public Contact getCaller() {
        return this.caller;
    }

    public void setCallee(Contact callee) {
        this.callee = callee;
    }

    public Contact getCallee() {
        return this.callee;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        if (null != this.caller) {
            json.put("caller", this.caller.toBasicJSON());
        }
        if (null != this.callee) {
            json.put("callee", this.callee.toBasicJSON());
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
