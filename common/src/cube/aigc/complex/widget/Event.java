/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.complex.widget;

import cube.aigc.complex.attachment.Attachment;
import cube.common.JSONable;
import cube.common.entity.AttachmentResource;
import cube.common.entity.ComplexResource;
import org.json.JSONObject;

/**
 * 事件描述。
 */
public class Event implements JSONable {

    public long resourceSn;

    public long attachmentId;

    public long componentId;

    public String name;

    public JSONObject parameter;

    public AttachmentResource resource;

    public Attachment attachment;

    public Widget target;

    public ComplexResource resultResource;

    public Event() {
    }

    public Event(JSONObject json) {
        this.resourceSn = json.getLong("resourceSn");
        this.attachmentId = json.getLong("attachmentId");
        this.componentId = json.getLong("componentId");
        this.name = json.getString("name");
        if (json.has("parameter")) {
            this.parameter = json.getJSONObject("parameter");
        }
    }

    public void finish(ComplexResource resource) {
        this.resultResource = resource;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("resourceSn", this.resourceSn);
        json.put("attachmentId", this.attachmentId);
        json.put("componentId", this.componentId);
        json.put("name", this.name);
        if (null != this.parameter) {
            json.put("parameter", this.parameter);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = this.toJSON();
        if (json.has("parameter")) {
            json.remove("parameter");
        }
        return json;
    }
}
