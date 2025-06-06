/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.aigc.complex.attachment.Attachment;
import cube.aigc.complex.attachment.ThingAttachment;
import cube.aigc.complex.attachment.ReportAttachment;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 附件资源。
 */
public class AttachmentResource extends ComplexResource {

    private List<Attachment> attachments;

    public AttachmentResource(Attachment attachment) {
        super(Subject.Attachment);
        this.attachments = new ArrayList<>();
        this.attachments.add(attachment);
    }

    public AttachmentResource(JSONObject json) {
        super(Subject.Attachment, json);
        this.attachments = new ArrayList<>();

        JSONObject payload = json.getJSONObject("payload");

        JSONArray attachmentArray = payload.getJSONArray("attachments");
        for (int i = 0; i < attachmentArray.length(); ++i) {
            JSONObject data = attachmentArray.getJSONObject(i);
            String type = data.getString("type");
            if (type.equals(ReportAttachment.TYPE)) {
                ReportAttachment report = new ReportAttachment(data);
                this.attachments.add(report);
            }
            else if (type.equals(ThingAttachment.TYPE)) {
                ThingAttachment thing = new ThingAttachment(data);
                this.attachments.add(thing);
            }
        }
    }

    public Attachment getAttachment() {
        if (this.attachments.isEmpty()) {
            return null;
        }
        return this.attachments.get(0);
    }

    public Attachment getAttachment(long attachmentId) {
        for (Attachment attachment : this.attachments) {
            if (attachment.getId() == attachmentId) {
                return attachment;
            }
        }

        return null;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();

        JSONObject payload = new JSONObject();
        JSONArray attachmentArray = new JSONArray();
        for (Attachment attachment : this.attachments) {
            attachmentArray.put(attachment.toJSON());
        }
        payload.put("attachments", attachmentArray);

        json.put("payload", payload);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
