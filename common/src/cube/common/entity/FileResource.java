/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import org.json.JSONObject;

/**
 * 文件资源。
 */
public class FileResource extends ComplexResource {

    private FileLabel fileLabel;

    public FileResource(FileLabel fileLabel) {
        super(Subject.File);
        this.fileLabel = fileLabel;
    }

    protected FileResource(JSONObject json) {
        super(Subject.File, json);
        this.fileLabel = new FileLabel(json.getJSONObject("fileLabel"));
    }

    public FileLabel getFileLabel() {
        return this.fileLabel;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("fileLabel", this.fileLabel.toJSON());
        return json;
    }
}
