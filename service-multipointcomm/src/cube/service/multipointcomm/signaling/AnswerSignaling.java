/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm.signaling;

import cube.common.action.MultipointCommAction;
import cube.common.entity.CommField;
import cube.common.entity.CommFieldEndpoint;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import org.json.JSONObject;

/**
 * Answer 信令。
 */
public class AnswerSignaling extends Signaling {

    private JSONObject sessionDescription;

    private JSONObject mediaConstraint;

    private Contact caller;

    private Contact callee;

    public AnswerSignaling(Long sn, CommField field, CommFieldEndpoint endpoint) {
        super(sn, MultipointCommAction.Answer.name, field, endpoint.getContact(), endpoint.getDevice());
    }

    public AnswerSignaling(CommField field, Contact contact, Device device) {
        super(MultipointCommAction.Answer.name, field, contact, device);
    }

    public AnswerSignaling(JSONObject json) {
        super(json);

        this.sessionDescription = json.getJSONObject("description");

        if (json.has("constraint")) {
            this.mediaConstraint = json.getJSONObject("constraint");
        }

        if (json.has("caller")) {
            this.caller = new Contact(json.getJSONObject("caller"));
        }
        if (json.has("callee")) {
            this.caller = new Contact(json.getJSONObject("callee"));
        }
    }

    public void copy(AnswerSignaling source) {
        this.caller = source.caller;
        this.callee = source.callee;
        this.sessionDescription = source.sessionDescription;
        this.mediaConstraint = source.mediaConstraint;
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

    public JSONObject getSessionDescription() {
        return this.sessionDescription;
    }

    public JSONObject getMediaConstraint() {
        return this.mediaConstraint;
    }

    public String getType() {
        return this.sessionDescription.getString("type");
    }

    public String getSDP() {
        return this.sessionDescription.getString("sdp");
    }

    public void setSDP(String sdp) {
        this.sessionDescription = new JSONObject();
        this.sessionDescription.put("type", "answer");
        this.sessionDescription.put("sdp", sdp);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();

        json.put("description", this.sessionDescription);

        if (null != this.mediaConstraint) {
            json.put("constraint", this.mediaConstraint);
        }

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
