/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

import cell.util.Utils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 文件缩略图。
 */
public class FileThumbnail extends Entity {

    private FileLabel fileLabel;

    private int width;

    private int height;

    private String sourceFileCode;

    private int sourceWidth;

    private int sourceHeight;

    private double quality;

    public FileThumbnail(FileLabel fileLabel, int width, int height,
                         String sourceFileCode, int sourceWidth, int sourceHeight,
                         double quality) {
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
            this.quality = json.getDouble("quality");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean equals(Object object) {
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
