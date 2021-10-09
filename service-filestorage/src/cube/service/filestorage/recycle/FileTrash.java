/*
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

package cube.service.filestorage.recycle;

import cube.common.entity.FileLabel;
import cube.service.filestorage.hierarchy.Directory;
import org.json.JSONObject;

/**
 * 垃圾文件。
 */
public class FileTrash extends Trash {

    private Directory parent;

    private FileLabel fileLabel;

    public FileTrash(Directory root, RecycleChain chain, FileLabel fileLabel) {
        super(root, chain, fileLabel.getId());
        this.fileLabel = fileLabel;
        this.parent = chain.getLast();
    }

    public FileTrash(Directory root, JSONObject json) {
        super(root, json);
        this.parent = getChain().getLast();
        this.fileLabel = new FileLabel(json.getJSONObject("file"));
    }

    @Override
    public Directory getParent() {
        return this.parent;
    }

    public String getFileCode() {
        return this.fileLabel.getFileCode();
    }

    public FileLabel getFileLabel() {
        return this.fileLabel;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("file", this.fileLabel.toCompactJSON());
        return json;
    }
}
