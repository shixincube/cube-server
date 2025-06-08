/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.util.JSONUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库文档。
 */
public class KnowledgeDocument extends Entity {

    /**
     * 自动语义切割。
     */
    public final static String SPLITTER_AUTO = "Auto";

    /**
     * 根据中文标点符号切割。
     */
    public final static String SPLITTER_PUNCTUATION = "Punctuation";

    /**
     * 根据文本符号行进行切割。
     */
    public final static String SPLITTER_SIMPLE = "Simple";

    /**
     * 根据文本行进行切割，即一行文本切割为一段。
     */
    public final static String SPLITTER_LINE = "Line";

    public final long contactId;

    public final String fileCode;

    /**
     * 文档所属库名。
     */
    public String baseName = "document";

    public String fileName;

    public boolean activated;

    private FileLabel fileLabel;

    /**
     * 文本分割器。
     */
    public String splitter = SPLITTER_LINE;

    /**
     * 分割的内容段落数量。
     */
    public int numSegments = -1;

    /**
     * 作用范围。
     */
    public KnowledgeScope scope = KnowledgeScope.Private;

    /**
     * 知识标签。
     */
    public List<String> knowledgeLabels = new ArrayList<>();

    public KnowledgeDocument(long id, String domain, long contactId, String fileCode, String baseName,
                             String fileName, boolean activated, int numSegments, KnowledgeScope scope) {
        super(id, domain);
        this.contactId = contactId;
        this.fileCode = fileCode;
        if (null != baseName) {
            this.baseName = baseName;
        }
        this.fileName = fileName;
        this.activated = activated;
        this.numSegments = numSegments;
        this.scope = scope;

        if (null != fileName) {
            this.knowledgeLabels.add(fileName.split("\\.")[0]);
        }
    }

    public KnowledgeDocument(JSONObject json) {
        super(json);
        this.contactId = json.getLong("contactId");
        this.fileCode = json.getString("fileCode");
        this.baseName = json.getString("baseName");
        this.activated = json.getBoolean("activated");
        this.splitter = json.getString("splitter");
        this.numSegments = json.getInt("numSegments");
        this.scope = KnowledgeScope.parse(json.getString("scope"));

        if (json.has("fileName")) {
            this.fileName = json.getString("fileName");
        }

        if (json.has("fileLabel")) {
            this.fileLabel = new FileLabel(json.getJSONObject("fileLabel"));
        }

        if (json.has("knowledgeLabels")) {
            this.knowledgeLabels = JSONUtils.toStringList(json.getJSONArray("knowledgeLabels"));
        }
    }

    public FileLabel getFileLabel() {
        return this.fileLabel;
    }

    public void setFileLabel(FileLabel fileLabel) {
        this.fileLabel = fileLabel;
        this.fileName = fileLabel.getFileName();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof KnowledgeDocument) {
            KnowledgeDocument other = (KnowledgeDocument) object;
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
        json.put("baseName", this.baseName);
        json.put("activated", this.activated);
        json.put("splitter", this.splitter);
        json.put("numSegments", this.numSegments);
        json.put("scope", this.scope.name);

        if (null != this.fileName) {
            json.put("fileName", this.fileName);
        }

        if (null != this.fileLabel) {
            json.put("fileLabel", this.fileLabel.toJSON());
        }

        json.put("knowledgeLabels", JSONUtils.toStringArray(this.knowledgeLabels));
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = this.toJSON();
        if (json.has("fileLabel")) {
            json.remove("fileLabel");
            json.put("fileLabel", this.fileLabel.toCompactJSON());
        }
        return json;
    }
}
