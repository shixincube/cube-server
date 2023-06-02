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

import org.json.JSONObject;

/**
 * 知识库文档。
 */
public class KnowledgeDoc extends Entity {

    public final long contactId;

    public final String fileCode;

    public final boolean activated;

    public FileLabel fileLabel;

    public KnowledgeDoc(long id, String domain, long contactId, String fileCode, boolean activated) {
        super(id, domain);
        this.contactId = contactId;
        this.fileCode = fileCode;
        this.activated = activated;
    }

    public KnowledgeDoc(JSONObject json) {
        super(json);
        this.contactId = json.getLong("contactId");
        this.fileCode = json.getString("fileCode");
        this.activated = json.getBoolean("activated");

        if (json.has("fileLabel")) {
            this.fileLabel = new FileLabel(json.getJSONObject("fileLabel"));
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof KnowledgeDoc) {
            KnowledgeDoc other = (KnowledgeDoc) object;
            return other.fileCode.equals(this.fileCode);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.fileCode.hashCode();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("contactId", this.contactId);
        json.put("fileCode", this.fileCode);
        json.put("activated", this.activated);

        if (null != this.fileLabel) {
            json.put("fileLabel", this.fileLabel.toJSON());
        }
        return json;
    }
}
