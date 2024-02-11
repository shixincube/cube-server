/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 知识库信息。
 */
public class KnowledgeBaseInfo implements JSONable {

    public long contactId;

    public String name;

    public String displayName;

    public String category;

    public long unitId;

    public long storeSize;

    public long timestamp;

    public KnowledgeBaseInfo(long contactId, String name, String displayName, String category,
                             long unitId, long storeSize, long timestamp) {
        this.contactId = contactId;
        this.name = name;
        this.displayName = displayName;
        this.category = category;
        this.unitId = unitId;
        this.storeSize = storeSize;
        this.timestamp = timestamp;
    }

    public KnowledgeBaseInfo(JSONObject json) {
        this.contactId = json.getLong("contactId");
        this.name = json.getString("name");
        this.displayName = json.getString("displayName");
        this.unitId = json.getLong("unitId");
        this.storeSize = json.getLong("storeSize");
        this.timestamp = json.getLong("timestamp");
        if (json.has("category") && json.getString("category").trim().length() > 0) {
            this.category = json.getString("category");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof KnowledgeBaseInfo) {
            KnowledgeBaseInfo other = (KnowledgeBaseInfo) obj;
            return other.contactId == this.contactId && other.name.equals(this.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (new Long(this.contactId)).hashCode() * 7 - this.name.hashCode();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("contactId", this.contactId);
        json.put("name", this.name);
        json.put("displayName", this.displayName);
        json.put("unitId", this.unitId);
        json.put("storeSize", this.storeSize);
        json.put("timestamp", this.timestamp);
        if (null != this.category && this.category.length() > 0) {
            json.put("category", this.category);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
