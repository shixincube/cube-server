/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import cube.file.misc.MediaAttribute;
import org.json.JSONObject;

import java.io.File;

/**
 * 处理结果流描述。
 */
public class FileResult implements JSONable {

    public File file;

    public String fullPath;

    public String streamName;

    public String fileName;

    public long fileSize;

    public MediaAttribute mediaAttribute;

    public FileResult(File file) {
        this.file = file;
        this.fullPath = file.getAbsolutePath();
        this.fileName = file.getName();
        this.fileSize = file.length();
        this.streamName = this.fileName;
    }

    public FileResult(JSONObject json) {
        this.fullPath = json.getString("fullPath");
        this.streamName = json.getString("streamName");
        this.fileName = json.getString("fileName");
        this.fileSize = json.getLong("fileSize");
        this.file = new File(this.fullPath);

        if (json.has("mediaAttribute")) {
            this.mediaAttribute = new MediaAttribute(json.getJSONObject("mediaAttribute"));
        }
    }

    public void resetFile(File file) {
        this.file = file;
        this.fullPath = file.getAbsolutePath();
        this.fileName = file.getName();
        this.fileSize = file.length();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("fullPath", this.fullPath);
        json.put("streamName", this.streamName);
        json.put("fileName", this.fileName);
        json.put("fileSize", this.fileSize);

        if (null != this.mediaAttribute) {
            json.put("mediaAttribute", this.mediaAttribute.toJSON());
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
