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
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件层级描述类。
 */
public class FileHierarchy {

    protected final static String KEY_CREATION = "creation";
    protected final static String KEY_LAST_MODIFIED = "lastModified";
    protected final static String KEY_DIR_NAME = "name";

    protected final static String KEY_RESERVED = "reserved";

    private Cache cache;

    private Directory root;

    private ConcurrentHashMap<Long, Directory> directories;

    public FileHierarchy(Cache cache, HierarchyNode root) {
        this.cache = cache;
        this.root = new Directory(this, root);
        this.directories = new ConcurrentHashMap<>();
    }

    public Directory getRoot() {
        return this.root;
    }

    /**
     * 是否存在同名目录。
     *
     * @param directory
     * @param directoryName
     * @return
     */
    protected boolean existsDirectory(Directory directory, String directoryName) {
        try {
            List<HierarchyNode> children = HierarchyNodes.traversalChildren(this.cache, directory.node);
            for (HierarchyNode node : children) {
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

    /**
     * 是否存在相同目录。
     *
     * @param parentNode
     * @param child
     * @return
     */
    protected boolean existsDirectory(HierarchyNode parentNode, Directory child) {
        List<HierarchyNode> children = HierarchyNodes.traversalChildren(this.cache, parentNode);
        for (HierarchyNode node : children) {
            if (node.equals(child.node)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 新建目录。
     *
     * @param directory
     * @param directoryName
     * @return
     */
    protected Directory createDirectory(Directory directory, String directoryName) {
        if (this.existsDirectory(directory, directoryName)) {
            // 目录重名，不允许创建
            return null;
        }

        long now = System.currentTimeMillis();

        // 创建目录
        HierarchyNode dirNode = new HierarchyNode(directory.node);
        try {
            JSONObject context = dirNode.getContext();
            context.put(KEY_DIR_NAME, directoryName);
            context.put(KEY_CREATION, now);
            context.put(KEY_LAST_MODIFIED, now);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // 添加为子节点
        directory.node.addChild(dirNode);

        HierarchyNodes.save(this.cache, dirNode);
        HierarchyNodes.save(this.cache, directory.node);

        Directory dir = new Directory(this, dirNode);
        this.directories.put(dir.getId(), dir);

        return dir;
    }

    /**
     * 删除目录。
     *
     * @param directory
     * @param subdirectory
     * @return
     */
    protected boolean deleteDirectory(Directory directory, Directory subdirectory) {
        if (this.root.equals(subdirectory)) {
            // 不能删除根
            return false;
        }

        if (!this.existsDirectory(directory.node, subdirectory)) {
            // 没有这个子目录
            return false;
        }

        // 目录里是否还有数据
        if (0 != subdirectory.node.numRelatedKeys()) {
            // 不允许删除非空目录
            return false;
        }

        HierarchyNode parent = directory.node;
        if (parent.getId().longValue() != subdirectory.node.getParent().getId().longValue()) {
            // 不是父子关系节点
            return false;
        }

        parent.removeChild(subdirectory.node);

        // 更新时间戳
        try {
            directory.node.getContext().put(KEY_LAST_MODIFIED, System.currentTimeMillis());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        HierarchyNodes.save(this.cache, parent);
        HierarchyNodes.delete(this.cache, subdirectory.node);

        this.directories.remove(subdirectory.getId());

        return true;
    }

    /**
     * 获取指定目录的所有子目录。
     *
     * @param directory
     * @return
     */
    protected List<Directory> getSubdirectories(Directory directory) {
        List<Directory> result = new ArrayList<>();

        List<HierarchyNode> nodes = HierarchyNodes.traversalChildren(this.cache, directory.node);
        for (HierarchyNode node : nodes) {
            Directory dir = this.directories.get(node.getId());
            if (null != dir) {
                result.add(dir);
                continue;
            }

            dir = new Directory(this, node);
            this.directories.put(dir.getId(), dir);
            result.add(dir);
        }

        return result;
    }
}
