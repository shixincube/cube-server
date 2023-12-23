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

    /**
     * 自动语义切割。
     */
    public final static String SPLITTER_AUTO = "Auto";

    /**
     * 根据标点符号切割。
     */
    public final static String SPLITTER_PUNCTUATION = "Punctuation";

    /**
     * 根据文本行进行切割，即一行文本切割为一段。
     */
    public final static String SPLITTER_LINE = "Line";

    public final long contactId;

    public final String fileCode;

    public final boolean activated;

    public FileLabel fileLabel;

    /**
     * 文本分割器。
     */
    public String splitter = SPLITTER_AUTO;

    /**
     * 分割的内容段落数量。
     */
    public int numSegments = -1;

    /**
     * 作用范围。
     */
    public KnowledgeScope scope = KnowledgeScope.Private;

    public KnowledgeDoc(long id, String domain, long contactId, String fileCode, boolean activated,
                        int numSegments, KnowledgeScope scope) {
        super(id, domain);
        this.contactId = contactId;
        this.fileCode = fileCode;
        this.activated = activated;
        this.numSegments = numSegments;
        this.scope = scope;
    }

    public KnowledgeDoc(JSONObject json) {
        super(json);
        this.contactId = json.getLong("contactId");
        this.fileCode = json.getString("fileCode");
        this.activated = json.getBoolean("activated");
        this.splitter = json.getString("splitter");
        this.numSegments = json.getInt("numSegments");
        this.scope = KnowledgeScope.parse(json.getString("scope"));

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
        json.put("splitter", this.splitter);
        json.put("numSegments", this.numSegments);
        json.put("scope", this.scope.name);

        if (null != this.fileLabel) {
            json.put("fileLabel", this.fileLabel.toJSON());
        }
        return json;
    }
}
