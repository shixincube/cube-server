/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
 */

package cube.common.entity;

import cube.vision.Size;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 物体信息。
 */
public class ObjectInfo extends Entity {

    private String fileCode;

    private FileLabel fileLabel;

    private Size size;

    private List<Material> materials;

    public ObjectInfo(JSONObject json) {
        this.fileCode = json.getString("fileCode");
        this.size = new Size(json.getJSONObject("size"));
        this.materials = new ArrayList<>();
        JSONArray array = json.getJSONArray("materials");
        for (int i = 0; i < array.length(); ++i) {
            this.materials.add(new Material(array.getJSONObject(i)));
        }

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

    public List<Material> getObjects() {
        return this.materials;
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
        JSONArray array = new JSONArray();
        for (Material material : this.materials) {
            array.put(material.toJSON());
        }
        json.put("materials", array);

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
