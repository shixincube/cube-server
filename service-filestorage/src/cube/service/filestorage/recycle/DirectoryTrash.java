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

package cube.service.filestorage.recycle;

import cube.service.filestorage.hierarchy.Directory;
import cube.service.filestorage.hierarchy.FileHierarchyTool;
import org.json.JSONObject;

/**
 * 垃圾目录。
 */
public class DirectoryTrash extends Trash {

    private Directory directory;

    public DirectoryTrash(Directory root, RecycleChain chain, Directory directory) {
        super(root, chain, directory.getId());
        this.directory = directory;
    }

    public DirectoryTrash(Directory root, JSONObject json) {
        super(root, json);
        this.directory = FileHierarchyTool.unpackDirectory(json.getJSONObject("directory"));
    }

    @Override
    public Directory getParent() {
        return this.directory.getParent();
    }

    public Directory getDirectory() {
        return this.directory;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        JSONObject dirJson = this.directory.toCompactJSON();
        FileHierarchyTool.packDirectory(this.directory, dirJson);
        json.put("directory", dirJson);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        JSONObject dirJson = this.directory.toCompactJSON();
        FileHierarchyTool.packDirectory(this.directory, dirJson);
        json.put("directory", dirJson);
        return json;
    }
}
