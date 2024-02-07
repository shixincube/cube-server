/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

import cube.common.JSONable;
import cube.util.FileType;
import cube.util.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件附件。
 */
public class FileAttachment implements JSONable {

    /**
     * 锚点。
     */
    private List<FileAnchor> anchorList;

    /**
     * 文件标签。
     */
    private List<FileLabel> labelList;

    /**
     * 是否压缩了原文件。
     */
    private boolean compressed = false;

    /**
     * 构造函数。
     *
     * @param json 附件的 JSON 结构。
     */
    public FileAttachment(JSONObject json) {
        this.anchorList = new ArrayList<>();
        this.labelList = new ArrayList<>();

        if (json.has("labels")) {
            JSONArray array = json.getJSONArray("labels");
            for (int i = 0; i < array.length(); ++i) {
                FileLabel fileLabel = new FileLabel(array.getJSONObject(i));
                this.labelList.add(fileLabel);
            }
        }

        if (json.has("anchors")) {
            JSONArray array = json.getJSONArray("anchors");
            for (int i = 0; i < array.length(); ++i) {
                FileAnchor fileAnchor = new FileAnchor(array.getJSONObject(i));
                this.anchorList.add(fileAnchor);
            }
        }

        if (json.has("compressed")) {
            this.compressed = json.getBoolean("compressed");
        }
    }

    /**
     * 构造函数。
     *
     * @param fileLabel 指定文件标签。
     */
    public FileAttachment(FileLabel fileLabel) {
        this.anchorList = new ArrayList<>();
        this.labelList = new ArrayList<>();
        this.labelList.add(fileLabel);
    }

    /**
     * 获取包含的文件数量。
     *
     * @return
     */
    public int numFiles() {
        return Math.max(this.anchorList.size(), this.labelList.size());
    }

    /**
     * 返回文件码。
     *
     * @return 返回文件码。
     */
    public String getFileCode(int index) {
        if (index < this.anchorList.size()) {
            return this.anchorList.get(index).fileCode;
        }

        if (index < this.labelList.size()) {
            return this.labelList.get(index).getFileCode();
        }

        return null;
    }

    /**
     * 返回文件标签。
     *
     * @return 返回文件标签。
     */
    public FileLabel getFileLabel(int index) {
        return this.labelList.get(index);
    }

    /**
     * 设置文件标签。
     *
     * @param fileLabel 指定文件标签。
     */
    public void addFileLabel(FileLabel fileLabel) {
        int index = this.labelList.indexOf(fileLabel);
        if (index >= 0) {
            this.labelList.set(index, fileLabel);
            return;
        }

        this.labelList.add(fileLabel);
    }

    public boolean isCompressed() {
        return this.compressed;
    }

    public void setCompressed(boolean value) {
        this.compressed = value;
    }

    public boolean isImageType(int index) {
        FileType type = null;

        if (index < this.labelList.size()) {
            FileLabel fileLabel = this.labelList.get(index);
            type = fileLabel.getFileType();
        }

        if (null == type && index < this.anchorList.size()) {
            FileAnchor anchor = this.anchorList.get(index);
            type = FileUtils.extractFileExtensionType(anchor.fileName);
        }

        if (null == type) {
            return false;
        }

        return (FileType.JPEG == type || FileType.PNG == type || FileType.BMP == type || FileType.GIF == type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        JSONArray array = new JSONArray();
        for (FileAnchor anchor : this.anchorList) {
            array.put(anchor.toJSON());
        }
        json.put("anchors", array);

        array = new JSONArray();
        for (FileLabel label : this.labelList) {
            array.put(label.toJSON());
        }
        json.put("labels", array);

        json.put("compressed", this.compressed);

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
