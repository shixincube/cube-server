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
import cube.common.entity.HierarchyNode;
import cube.common.entity.HierarchyNodes;
import cube.core.Cache;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件层级描述类。
 */
public class FileHierarchy {

    protected final static String KEY_CREATION = "creation";
    protected final static String KEY_LAST_MODIFIED = "lastModified";
    protected final static String KEY_DIR_NAME = "name";

    private Cache cache;

    private HierarchyNode root;

    public FileHierarchy(Cache cache, HierarchyNode root) {
        this.cache = cache;
        this.root = root;

        try {
            JSONObject context = this.root.getContext();
            if (!context.has(KEY_CREATION)) {
                context.put(KEY_CREATION, System.currentTimeMillis());
                context.put(KEY_LAST_MODIFIED, System.currentTimeMillis());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public HierarchyNode getRoot() {
        return this.root;
    }

    public String getDirectoryName(HierarchyNode directory) {
        try {
            return directory.getContext().getString(KEY_DIR_NAME);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public HierarchyNode createDirectory(HierarchyNode hierarchy, String directoryName) {
        if (this.existsDirectory(hierarchy, directoryName)) {
            // 目录重名，不允许创建
            return null;
        }

        long now = System.currentTimeMillis();

        // 创建目录
        HierarchyNode dirNode = new HierarchyNode(hierarchy);
        try {
            JSONObject context = dirNode.getContext();
            context.put(KEY_DIR_NAME, directoryName);
            context.put(KEY_CREATION, now);
            context.put(KEY_LAST_MODIFIED, now);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // 添加为子节点
        hierarchy.addChild(dirNode);

        HierarchyNodes.save(cache, dirNode);
        HierarchyNodes.save(cache, hierarchy);

        return dirNode;
    }

    public boolean deleteDirectory(HierarchyNode directory) {
        if (this.root.equals(directory)) {
            // 不能删除根
            return false;
        }

        HierarchyNode root = HierarchyNodes.traversalUp(directory, 0);
        if (!root.equals(this.root)) {
            // 不是同一个根
            return false;
        }

        // 目录里是否还有数据
        if (0 != directory.numRelatedKeys()) {
            // 不允许删除非空目录
            return false;
        }

        HierarchyNode parent = directory.getParent();
        parent.removeChild(directory);

        HierarchyNodes.save(cache, parent);
        HierarchyNodes.delete(cache, directory);

        return true;
    }

    public List<HierarchyNode> getSubdirectories(HierarchyNode directory) {
        return null;
    }

    /**
     * 是否存在同名目录。
     *
     * @param hierarchy
     * @param directoryName
     * @return
     */
    public boolean existsDirectory(HierarchyNode hierarchy, String directoryName) {
        try {
            for (HierarchyNode node : hierarchy.getChildren()) {
                JSONObject context = node.getContext();
                String dirName = context.getString(KEY_DIR_NAME);
                if (dirName.equals(directoryName)) {
                    return true;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return false;
    }
}
