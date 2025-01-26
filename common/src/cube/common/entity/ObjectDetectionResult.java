/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import cube.util.TimeOffset;
import cube.util.TimeUtils;
import cube.vision.Size;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 物体检测结果。
 */
public class ObjectDetectionResult implements JSONable {

    public final String fileCode;

    public long elapsed;

    public Size size;

    public List<Material> materials;

    public String description;

    public FileLabel markedFile;

    public ObjectDetectionResult(JSONObject json) {
        this.fileCode = json.getString("fileCode");
        this.size = new Size(json.getJSONObject("size"));
        this.elapsed = json.getLong("elapsed");
        this.materials = new ArrayList<>();
        JSONArray array = json.getJSONArray("materials");
        for (int i = 0; i < array.length(); ++i) {
            this.materials.add(new Material(array.getJSONObject(i)));
        }

        if (json.has("markedFile")) {
            this.markedFile = new FileLabel(json.getJSONObject("markedFile"));
        }

        if (json.has("description")) {
            this.description = json.getString("description");
        }
        else {
            this.description = this.makeDescription(this.materials);
        }
    }

    /**
     * 生成 Markdown 格式的描述。
     *
     * @param list
     * @return
     */
    private String makeDescription(List<Material> list) {
        StringBuilder buf = new StringBuilder();
        buf.append("图片中包含以下物品：\n");
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
        json.put("size", this.size.toJSON());
        json.put("elapsed", this.elapsed);
        JSONArray array = new JSONArray();
        for (Material material : this.materials) {
            array.put(material.toJSON());
        }
        json.put("materials", array);

        if (null != this.markedFile) {
            json.put("markedFile", this.markedFile.toCompactJSON());
        }

        json.put("description", this.description);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
