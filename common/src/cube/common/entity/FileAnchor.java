/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 文件锚点。
 */
public class FileAnchor implements JSONable {

    public final String fileCode;

    public final String fileName;

    public final long fileSize;

    public final long lastModified;

    public final long position;

    public FileAnchor(JSONObject json) {
        this.fileCode = json.getString("fileCode");
        this.fileName = json.getString("fileName");
        this.fileSize = json.getLong("fileSize");
        this.lastModified = json.getLong("lastModified");
        this.position = json.getLong("position");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("fileCode", this.fileCode);
        json.put("fileName", this.fileName);
        json.put("fileSize", this.fileSize);
        json.put("lastModified", this.lastModified);
        json.put("position", this.position);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
