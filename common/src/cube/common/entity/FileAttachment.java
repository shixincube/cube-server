/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cube.common.JSONable;

import java.util.List;

/**
 * 文件附件。
 */
public class FileAttachment implements JSONable {

    /**
     * JSON 格式描述的锚点。
     */
    private JSONObject fileAnchor;

    /**
     * 文件标签。
     */
    private FileLabel fileLabel;

    /**
     * 文件缩略图列表。
     */
    private List<FileThumbnail> thumbList;

    /**
     * 构造函数。
     *
     * @param json 附件的 JSON 结构。
     */
    public FileAttachment(JSONObject json) {
        try {
            if (json.has("anchor")) {
                this.fileAnchor = json.getJSONObject("anchor");
            }
            if (json.has("label")) {
                this.fileLabel = new FileLabel(json.getJSONObject("label"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 返回文件码。
     *
     * @return 返回文件码。
     */
    public String getFileCode() {
        if (null != this.fileAnchor) {
            try {
                return this.fileAnchor.getString("fileCode");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (null != this.fileLabel) {
            return this.fileLabel.getFileCode();
        }

        return null;
    }

    /**
     * 返回文件标签。
     *
     * @return 返回文件标签。
     */
    public FileLabel getFileLabel() {
        return this.fileLabel;
    }

    /**
     * 设置文件标签。
     *
     * @param fileLabel 指定文件标签。
     */
    public void setFileLabel(FileLabel fileLabel) {
        this.fileLabel = fileLabel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        if (null != this.fileAnchor) {
            try {
                json.put("anchor", this.fileAnchor);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (null != this.fileLabel) {
            try {
                json.put("label", this.fileLabel.toJSON());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return json;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
