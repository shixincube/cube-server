/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.common.entity;

import cube.aigc.attachment.Attachment;
import cube.aigc.attachment.ThingAttachment;
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
        super(Subject.Attachment);
        this.attachments = new ArrayList<>();

        JSONObject payload = json.getJSONObject("payload");

        JSONArray attachmentArray = payload.getJSONArray("attachments");
        for (int i = 0; i < attachmentArray.length(); ++i) {
            JSONObject data = attachmentArray.getJSONObject(i);
            String type = data.getString("type");
            if (type.equals(ThingAttachment.TYPE)) {
                ThingAttachment thing = new ThingAttachment(data);
                this.attachments.add(thing);
            }
        }
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
