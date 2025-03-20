/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
 */

package cube.common.entity;

import cube.vision.Size;
import org.json.JSONObject;

/**
 * 已加工的文件。
 */
public class ProcessedFile extends Entity {

    private String fileCode;

    private FileLabel fileLabel;

    private Size size;

    private FileLabel processed;

    public ProcessedFile(JSONObject json) {
        this.fileCode = json.getString("fileCode");
        this.size = new Size(json.getJSONObject("size"));
        this.processed = new FileLabel(json.getJSONObject("processed"));
        if (json.has("fileLabel")) {
            this.fileLabel = new FileLabel(json.getJSONObject("fileLabel"));
        }
    }

    public String getFileCode() {
        return this.fileCode;
    }

    public Size getSize() {
        return this.size;
    }

    public FileLabel getProcessed() {
        return this.processed;
    }

    public FileLabel getFileLabel() {
        return this.fileLabel;
    }

    public void setFileLabel(FileLabel fileLabel) {
        this.fileLabel = fileLabel;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("fileCode", this.fileCode);
        json.put("size", this.size.toJSON());
        json.put("processed", this.processed.toJSON());

        if (null != this.fileLabel) {
            json.put("fileLabel", this.fileLabel.toJSON());
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
