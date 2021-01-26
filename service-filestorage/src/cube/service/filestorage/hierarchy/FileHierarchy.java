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

package cube.service.filestorage.hierarchy;

import cube.common.entity.FileLabel;
import cube.common.entity.HierarchyNode;
import cube.common.entity.HierarchyNodes;
import cube.core.Cache;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件层级描述类。
 */
public class FileHierarchy {

    /** 目录创建时间。 */
    protected final static String KEY_CREATION = "creation";
    /** 目录最后一次修改时间。 */
    protected final static String KEY_LAST_MODIFIED = "lastModified";
    /** 目录名。 */
    protected final static String KEY_DIR_NAME = "name";
    /** 是否隐藏目录。 */
    protected final static String KEY_HIDDEN = "hidden";
    /** 目录内文件占用的空间大小。 */
    protected final static String KEY_SIZE = "size";

    /**
     * 该文件层级允许存入的文件的总大小。
     */
    private long capacity = 1L * 1024L * 1024L * 1024L;

    /**
     * 用于读写节点的缓存。
     */
    private Cache cache;

    /**
     * 根目录。
     */
    private Directory root;

    /**
     * 所有目录。
     */
    private ConcurrentHashMap<Long, Directory> directories;

    /**
     * 时间戳。
     */
    private long timestamp;

    /**
     * 事件监听器。
     */
    private FileHierarchyListener listener;

    /**
     * 构造函数。
     *
     * @param cache 用于读写节点的缓存。
     * @param root 根目录。
     */
    public FileHierarchy(Cache cache, HierarchyNode root, FileHierarchyListener listener) {
        this.cache = cache;
        this.root = new Directory(this, root);
        this.listener = listener;
        this.directories = new ConcurrentHashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 获取 ID 。
     *
     * @return 返回 ID 。
     */
    public long getId() {
        return this.root.getId();
    }

    /**
     * 获取当前的容量。
     *
     * @return 返回当前的容量。
     */
    public long getCapacity() {
        return this.capacity;
    }

    /**
     * 获取当前时间戳。
     *
     * @return 返回当前时间戳。
     */
    public long getTimestamp() {
        return this.timestamp;
    }

    /**
     * 返回根目录。
     *
     * @return 返回根目录。
     */
    public Directory getRoot() {
        return this.root;
    }

    /**
     * 返回指定 ID 的目录。
     *
     * @param id
     * @return
     */
    public Directory getDirectory(Long id) {
        if (id.longValue() == this.root.getId().longValue()) {
            return this.root;
        }

        Directory dir = this.directories.get(id);
        if (null != dir) {
            return dir;
        }

        // 递归方式遍历
        HierarchyNode node = traversal(this.cache, this.root.node, id);
        if (null != node) {
            dir = new Directory(this, node);
            this.directories.put(dir.getId(), dir);
        }

        return dir;
    }

    private HierarchyNode traversal(Cache cache, HierarchyNode node, Long id) {
        List<HierarchyNode> children = HierarchyNodes.traversalChildren(cache, node);
        if (children.isEmpty()) {
            return null;
        }

        for (HierarchyNode child : children) {
            if (child.getId().longValue() == id.longValue()) {
                return child;
            }
            else {
                HierarchyNode result = traversal(cache, child, id);
                if (null != result) {
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * 是否存在同名目录。
     *
     * @param directory
     * @param directoryName
     * @return
     */
    protected boolean existsDirectory(Directory directory, String directoryName) {
        this.timestamp = System.currentTimeMillis();

        List<HierarchyNode> children = HierarchyNodes.traversalChildren(this.cache, directory.node);
        for (HierarchyNode node : children) {
            JSONObject context = node.getContext();
            String dirName = context.getString(KEY_DIR_NAME);
            if (dirName.equals(directoryName)) {
                return true;
            }
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
        this.timestamp = System.currentTimeMillis();

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

        this.timestamp = now;

        // 创建目录
        HierarchyNode dirNode = new HierarchyNode(directory.node);
        try {
            JSONObject context = dirNode.getContext();
            context.put(KEY_DIR_NAME, directoryName);
            context.put(KEY_CREATION, now);
            context.put(KEY_LAST_MODIFIED, now);
            context.put(KEY_HIDDEN, false);
            context.put(KEY_SIZE, 0);
        } catch (Exception e) {
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
     * 删除多个目录。
     *
     * @param directory
     * @param subdirectories
     * @param recursive
     * @return
     */
    protected List<Directory> deleteDirectories(Directory directory, List<Directory> subdirectories, boolean recursive) {
        HierarchyNode parent = directory.node;

        List<Directory> result = new ArrayList<>();

        for (Directory subdirectory : subdirectories) {
            if (this.root.equals(subdirectory)) {
                // 不能删除根
                continue;
            }

            if (!this.existsDirectory(directory.node, subdirectory)) {
                // 没有这个子目录
                continue;
            }

            // 目录里是否还有子目录和文件
            if (!recursive && (0 != subdirectory.node.numChildren() || 0 != subdirectory.node.numRelatedKeys())) {
                // 不允许删除非空目录
                continue;
            }

            // 回调
            this.listener.onDirectoryRemove(this, subdirectory);

            // 删除
            parent.removeChild(subdirectory.node);

            // 从内存里移除
            this.directories.remove(subdirectory.getId());

            // 更新缓存
            HierarchyNodes.delete(this.cache, subdirectory.node);

            // 记录被删除的目录
            result.add(subdirectory);
        }

        if (result.isEmpty()) {
            // 没有目录被删除
            return null;
        }

        // 更新时间戳
        this.timestamp = System.currentTimeMillis();
        parent.getContext().put(KEY_LAST_MODIFIED, this.timestamp);

        HierarchyNodes.save(this.cache, parent);

        return result;
    }

    /**
     * 删除目录。
     *
     * @param directory
     * @param subdirectory
     * @param recursive
     * @return
     */
    protected boolean deleteDirectory(Directory directory, Directory subdirectory, boolean recursive) {
        if (this.root.equals(subdirectory)) {
            // 不能删除根
            return false;
        }

        if (!this.existsDirectory(directory.node, subdirectory)) {
            // 没有这个子目录
            return false;
        }

        // 目录里是否还有子目录和文件
        if (!recursive && (0 != subdirectory.node.numChildren() || 0 != subdirectory.node.numRelatedKeys())) {
            // 不允许删除非空目录
            return false;
        }

        HierarchyNode parent = directory.node;
//        if (parent.getId().longValue() != subdirectory.node.getParent().getId().longValue()) {
//            // 不是父子关系节点
//            return false;
//        }

        // 删除
        parent.removeChild(subdirectory.node);

        // 更新时间戳
        this.timestamp = System.currentTimeMillis();
        parent.getContext().put(KEY_LAST_MODIFIED, this.timestamp);

        HierarchyNodes.save(this.cache, parent);
        HierarchyNodes.delete(this.cache, subdirectory.node);

        this.directories.remove(subdirectory.getId());

        // 回调
        this.listener.onDirectoryRemove(this, subdirectory);

        return true;
    }

    /**
     * 获取父目录。
     *
     * @param directory 指定目录。
     * @return 返回父目录实例，如果没有父目录返回 {@code null} 值。
     */
    protected Directory getParent(Directory directory) {
        HierarchyNode parentNode = directory.node.getParent();
        if (null == parentNode) {
            return null;
        }

        Directory parent = this.directories.get(parentNode.getId());
        if (null == parent) {
            parent = new Directory(this, parentNode);
            this.directories.put(parent.getId(), parent);
        }

        return parent;
    }

    /**
     * 获取指定目录的所有子目录。
     *
     * @param directory
     * @return
     */
    protected List<Directory> getSubdirectories(Directory directory) {
        return this.getSubdirectories(directory, false);
    }

    /**
     * 获取指定目录的子目录。
     *
     * @param directory
     * @param includeHidden
     * @return
     */
    protected List<Directory> getSubdirectories(Directory directory, boolean includeHidden) {
        this.timestamp = System.currentTimeMillis();

        List<Directory> result = new ArrayList<>();

        List<HierarchyNode> nodes = HierarchyNodes.traversalChildren(this.cache, directory.node);
        for (HierarchyNode node : nodes) {
            Directory dir = this.directories.get(node.getId());
            if (null == dir) {
                dir = new Directory(this, node);
                this.directories.put(dir.getId(), dir);
            }

            if (!dir.isHidden()) {
                result.add(dir);
            }
            else {
                if (includeHidden) {
                    result.add(dir);
                }
            }
        }

        return result;
    }

    /**
     * 获取指定名称的子目录。
     *
     * @param directory
     * @param subdirectory
     * @return
     */
    protected Directory getSubdirectory(Directory directory, String subdirectory) {
        this.timestamp = System.currentTimeMillis();

        List<HierarchyNode> nodes = HierarchyNodes.traversalChildren(this.cache, directory.node);
        for (HierarchyNode node : nodes) {
            Directory dir = this.directories.get(node.getId());
            if (null == dir) {
                dir = new Directory(this, node);
                this.directories.put(dir.getId(), dir);
            }

            if (dir.getName().equals(subdirectory)) {
                return dir;
            }
        }

        return null;
    }

    /**
     * 获取指定 ID 的子目录。
     *
     * @param directory
     * @param id
     * @return
     */
    protected Directory getSubdirectory(Directory directory, Long id) {
        this.timestamp = System.currentTimeMillis();

        List<HierarchyNode> nodes = HierarchyNodes.traversalChildren(this.cache, directory.node);
        for (HierarchyNode node : nodes) {
            Directory dir = this.directories.get(node.getId());
            if (null == dir) {
                dir = new Directory(this, node);
                this.directories.put(dir.getId(), dir);
            }

            if (dir.getId().longValue() == id.longValue()) {
                return dir;
            }
        }

        return null;
    }

    /**
     * 设置目录为隐藏目录。
     *
     * @param directory
     * @param hidden
     */
    protected void setHidden(Directory directory, boolean hidden) {
        // 更新时间戳
        this.timestamp = System.currentTimeMillis();

        directory.node.getContext().put(KEY_HIDDEN, hidden);
        directory.node.getContext().put(KEY_LAST_MODIFIED, this.timestamp);

        HierarchyNodes.save(this.cache, directory.node);
    }

    /**
     * 设置目录大小。
     *
     * @param directory
     * @param newSize
     */
    protected void setDirectorySize(Directory directory, long newSize) {
        // 更新时间戳
        this.timestamp = System.currentTimeMillis();

        directory.node.getContext().put(KEY_SIZE, newSize);
        directory.node.getContext().put(KEY_LAST_MODIFIED, this.timestamp);

        HierarchyNodes.save(this.cache, directory.node);
    }

    /**
     * 添加文件标签。
     *
     * @param directory
     * @param fileLabel
     * @return
     */
    protected boolean addFileLabel(Directory directory, FileLabel fileLabel) {
        if (!directory.node.link(fileLabel)) {
            // 链接失败
            return false;
        }

        // 更新时间戳
        this.timestamp = System.currentTimeMillis();

        this.listener.onFileLabelAdd(this, directory, fileLabel);

        // 更新大小
        long size = directory.node.getContext().getLong(KEY_SIZE);
        directory.node.getContext().put(KEY_SIZE, size + fileLabel.getFileSize());

        // 更新时间戳
        directory.node.getContext().put(KEY_LAST_MODIFIED, this.timestamp);

        HierarchyNodes.save(this.cache, directory.node);

        return true;
    }

    /**
     * 移除文件标签。
     *
     * @param directory
     * @param fileLabel
     * @return
     */
    protected boolean removeFileLabel(Directory directory, FileLabel fileLabel) {
        if (!directory.node.unlink(fileLabel)) {
            // 解除链接失败
            return false;
        }

        // 更新时间戳
        this.timestamp = System.currentTimeMillis();

        this.listener.onFileLabelRemove(this, directory, fileLabel);

        // 更新大小
        long size = directory.node.getContext().getLong(KEY_SIZE);
        directory.node.getContext().put(KEY_SIZE, size - fileLabel.getFileSize());

        // 更新时间戳
        directory.node.getContext().put(KEY_LAST_MODIFIED, this.timestamp);

        HierarchyNodes.save(this.cache, directory.node);

        return true;
    }

    protected List<FileLabel> listFileLabels(Directory directory, int beginIndex, int endIndex) {
        List<FileLabel> result = new ArrayList<>();

        List<String> ukeys = directory.node.getRelatedKeys(beginIndex, endIndex);
        if (null == ukeys) {
            return result;
        }

        for (String key : ukeys) {
            FileLabel fileLabel = this.listener.onQueryFileLabel(this, directory, key);
            if (null != fileLabel) {
                result.add(fileLabel);
            }
        }

        return result;
    }

    /**
     * 文件数量。
     *
     * @param directory
     * @return
     */
    protected int numFiles(Directory directory) {
        return directory.node.numRelatedKeys();
    }
}
