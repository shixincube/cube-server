/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cell.util.Utils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 文件缩略图。
 */
public class FileThumbnail extends Entity {

    private FileLabel fileLabel;

    /**
     * 缩略图宽度。
     */
    private int width;

    /**
     * 缩略图高度。
     */
    private int height;

    /**
     * 缩略图质量。
     */
    private int quality;

    /**
     * 源文件的文件码。
     */
    private String sourceFileCode;

    /**
     * 源文件宽度。
     */
    private int sourceWidth;

    /**
     * 源文件高度。
     */
    private int sourceHeight;

    public FileThumbnail(FileLabel fileLabel, int width, int height,
                         String sourceFileCode, int sourceWidth, int sourceHeight,
                         int quality) {
        super(Utils.generateSerialNumber(), fileLabel.getDomain());
        this.fileLabel = fileLabel;
        this.width = width;
        this.height = height;
        this.sourceFileCode = sourceFileCode;
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
        this.quality = quality;
    }

    public FileThumbnail(JSONObject json) {
        super(json);
        try {
            this.fileLabel = new FileLabel(json.getJSONObject("fileLabel"));
            this.width = json.getInt("width");
            this.height = json.getInt("height");
            this.sourceFileCode = json.getString("sourceFileCode");
            this.sourceWidth = json.getInt("sourceWidth");
            this.sourceHeight = json.getInt("sourceHeight");
            this.quality = json.getInt("quality");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public FileLabel getFileLabel() {
        return this.fileLabel;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (null != object && object instanceof FileThumbnail) {
            FileThumbnail other = (FileThumbnail) object;
            if (other.fileLabel.equals(this.fileLabel)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", this.getId());
            json.put("domain", this.getDomain().getName());
            json.put("fileLabel", this.fileLabel.toJSON());
            json.put("width", this.width);
            json.put("height", this.height);
            json.put("sourceFileCode", this.sourceFileCode);
            json.put("sourceWidth", this.sourceWidth);
            json.put("sourceHeight", this.sourceHeight);
            json.put("quality", this.quality);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
