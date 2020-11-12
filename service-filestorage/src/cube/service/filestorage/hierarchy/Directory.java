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

package cube.service.filestorage.hierarchy;

import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cube.common.Domain;
import cube.common.entity.HierarchyNode;

import java.util.List;

/**
 * 文件目录。
 */
public class Directory {

    private FileHierarchy fileHierarchy;

    protected HierarchyNode node;

    public Directory(FileHierarchy fileHierarchy, HierarchyNode node) {
        this.fileHierarchy = fileHierarchy;
        this.node = node;

        try {
            JSONObject context = node.getContext();
            if (!context.has(FileHierarchy.KEY_CREATION)) {
                long now = System.currentTimeMillis();
                context.put(FileHierarchy.KEY_CREATION, now);
                context.put(FileHierarchy.KEY_LAST_MODIFIED, now);
            }
            if (!context.has(FileHierarchy.KEY_DIR_NAME)) {
                context.put(FileHierarchy.KEY_DIR_NAME, "root");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Long getId() {
        return this.node.getId();
    }

    public Domain getDomain() {
        return this.node.getDomain();
    }

    public String getName() {
        try {
            return this.node.getContext().getString(FileHierarchy.KEY_DIR_NAME);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public int numSubdirectories() {
        return this.node.numChildren();
    }

    public List<Directory> getSubdirectories() {
        return this.fileHierarchy.getSubdirectories(this);
    }

    public boolean existsDirectory(String directoryName) {
        return this.fileHierarchy.existsDirectory(this, directoryName);
    }

    public Directory createDirectory(String directoryName) {
        return this.fileHierarchy.createDirectory(this, directoryName);
    }

    public boolean deleteDirectory(Directory subdirectory) {
        return this.fileHierarchy.deleteDirectory(this, subdirectory);
    }

    @Override
    public boolean equals(Object object) {
        if (null != object && object instanceof Directory) {
            Directory other = (Directory) object;
            return this.node.equals(other.node);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.node.hashCode();
    }
}
