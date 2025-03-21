/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
 */

package cube.common.entity;

import cube.util.TimeOffset;
import cube.util.TimeUtils;
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

    private long elapsed;

    private Size size;

    private List<Material> materials;

    private String description;

    public ObjectInfo(JSONObject json) {
        this.fileCode = json.getString("fileCode");
        this.elapsed = json.getLong("elapsed");
        this.size = new Size(json.getJSONObject("size"));
        this.materials = new ArrayList<>();
        JSONArray array = json.getJSONArray("materials");
        for (int i = 0; i < array.length(); ++i) {
            this.materials.add(new Material(array.getJSONObject(i)));
        }

        if (json.has("fileLabel")) {
            this.fileLabel = new FileLabel(json.getJSONObject("fileLabel"));
        }

        if (json.has("description")) {
            this.description = json.getString("description");
        }
        else {
            this.description = this.makeDescription(this.materials);
        }
    }

    public String getFileCode() {
        return this.fileCode;
    }

    public long getElapsed() {
        return this.elapsed;
    }

    public Size getSize() {
        return this.size;
    }

    public List<Material> getObjects() {
        return this.materials;
    }

    public String getDescription() {
        return this.description;
    }

    public FileLabel getFileLabel() {
        return this.fileLabel;
    }

    public void setFileLabel(FileLabel fileLabel) {
        this.fileLabel = fileLabel;
    }

    /**
     * 生成 Markdown 格式的描述。
     *
     * @param list
     * @return
     */
    private String makeDescription(List<Material> list) {
        StringBuilder buf = new StringBuilder();
        buf.append("图片中包含以下物品：\n\n");
        for (Material material : list) {
            String prob = String.format("%.2f", material.prob * 100);
            buf.append("- ").append(material.label);
            buf.append(" <font color=\"").append(material.color).append("\">").append("●</font>");
            buf.append(" *(").append(prob).append("%)*\n");
        }
        buf.append("\n");
        TimeOffset timeOffset = TimeUtils.calcTimeDuration(this.elapsed);
        buf.append("识别耗时**").append(timeOffset.toHumanString()).append("**，共检测出**")
                .append(materials.size()).append("**件物品。");
        return buf.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("fileCode", this.fileCode);
        json.put("elapsed", this.elapsed);
        json.put("size", this.size.toJSON());
        JSONArray array = new JSONArray();
        for (Material material : this.materials) {
            array.put(material.toJSON());
        }
        json.put("materials", array);

        if (null != this.fileLabel) {
            json.put("fileLabel", this.fileLabel.toJSON());
        }

        if (null != this.description) {
            json.put("description", this.description);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
