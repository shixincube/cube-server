/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

import cube.common.Domain;
import cube.common.JSONable;
import cube.common.entity.FileLabel;
import cube.common.entity.HierarchyNode;
import cube.service.Director;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 文件目录。
 */
public class Directory implements JSONable {

    /**
     * 目录的文件层级容器。
     */
    private FileHierarchy fileHierarchy;

    /**
     * 目录对应的层级节点。
     */
    protected HierarchyNode node;

    /**
     * 构造函数。
     *
     * @param fileHierarchy 目录的文件层级容器。
     * @param node 目录对应的层级节点。
     */
    public Directory(FileHierarchy fileHierarchy, HierarchyNode node) {
        this.fileHierarchy = fileHierarchy;
        this.node = node;

        if (null != node) {
            JSONObject context = node.getContext();
            if (!context.has(FileHierarchy.KEY_CREATION)) {
                long now = System.currentTimeMillis();
                context.put(FileHierarchy.KEY_CREATION, now);
                context.put(FileHierarchy.KEY_LAST_MODIFIED, now);
            }
            if (!context.has(FileHierarchy.KEY_DIR_NAME)) {
                context.put(FileHierarchy.KEY_DIR_NAME, "root");
            }
            if (!context.has(FileHierarchy.KEY_HIDDEN)) {
                context.put(FileHierarchy.KEY_HIDDEN, false);
            }
            if (!context.has(FileHierarchy.KEY_SIZE)) {
                context.put(FileHierarchy.KEY_SIZE, 0L);
            }
        }
    }

    /**
     * 获取目录的 ID 。
     *
     * @return 返回目录的 ID 。
     */
    public Long getId() {
        return this.node.getId();
    }

    /**
     * 获取域。
     *
     * @return 返回域。
     */
    public Domain getDomain() {
        return this.node.getDomain();
    }

    /**
     * 获取目录名。
     *
     * @return 返回目录名。
     */
    public String getName() {
        return this.node.getContext().getString(FileHierarchy.KEY_DIR_NAME);
    }

    /**
     * 获取目录创建时间。
     *
     * @return 返回目录创建时间。
     */
    public long getCreationTime() {
        return this.node.getContext().getLong(FileHierarchy.KEY_CREATION);
    }

    /**
     * 获取目录最后一次修改时间。
     *
     * @return 返回目录最后一次修改时间。
     */
    public long getLastModified() {
        return this.node.getContext().getLong(FileHierarchy.KEY_LAST_MODIFIED);
    }

    /**
     * 是否是隐藏目录。
     *
     * @return 如果是隐藏目录返回 {@code true} 。
     */
    public boolean isHidden() {
        return this.node.getContext().getBoolean(FileHierarchy.KEY_HIDDEN);
    }

    /**
     * 获取目录占用的空间大小。
     *
     * @return 返回目录占用的空间大小。
     */
    public long getSize() {
        return this.node.getContext().getLong(FileHierarchy.KEY_SIZE);
    }

    /**
     * 获取父目录。
     *
     * @return 返回父目录。
     */
    public Directory getParent() {
        return this.fileHierarchy.getParent(this);
    }

    /**
     * 是否有父目录。
     *
     * @return 如果有父目录返回 {@code true} 。
     */
    public boolean hasParent() {
        return (null != this.fileHierarchy.getParent(this));
    }

    /**
     * 获取子目录数量。
     *
     * @return 返回子目录数量。
     */
    public int numDirectories() {
        return this.fileHierarchy.getSubdirectories(this).size();
    }

    /**
     * 获取所有子目录。
     *
     * @return 返回所有子目录。
     */
    public List<Directory> getDirectories() {
        return this.fileHierarchy.getSubdirectories(this);
    }

    /**
     * 获取所有子目录，包括隐藏目录。
     *
     * @return 返回所有子目录，包括隐藏目录。
     */
    public List<Directory> getAllDirectories() {
        return this.fileHierarchy.getSubdirectories(this, true);
    }

    /**
     * 获取指定名称的目录。
     *
     * @param directoryName 指定目录名。
     * @return 返回指定名称的目录。
     */
    public Directory getDirectory(String directoryName) {
        return this.fileHierarchy.getSubdirectory(this, directoryName);
    }

    /**
     * 获取指定 ID 的目录。
     *
     * @param id
     * @return
     */
    public Directory getDirectory(Long id) {
        return this.fileHierarchy.getSubdirectory(this, id);
    }

    /**
     * 是否存在指定名称的目录。
     *
     * @param directoryName 指定目录名。
     * @return 如果包含指定名称的目录返回 {@code true} 。
     */
    public boolean existsDirectory(String directoryName) {
        return this.fileHierarchy.existsDirectory(this, directoryName);
    }

    /**
     * 创建子目录。
     *
     * @param directoryName 指定目录名。
     * @return 返回新创建的子目录。
     */
    public Directory createDirectory(String directoryName) {
        if (directoryName.equals("root")) {
            // 不允许使用 root 作为目录名
            return null;
        }

        return this.fileHierarchy.createDirectory(this, directoryName, null);
    }

    /**
     * 创建子目录。
     *
     * @param directoryName 指定目录名。
     * @param directoryId 指定目录 ID 。
     * @return 返回新创建的子目录。
     */
    public Directory createDirectory(String directoryName, Long directoryId) {
        if (directoryName.equals("root")) {
            // 不允许使用 root 作为目录名
            return null;
        }

        return this.fileHierarchy.createDirectory(this, directoryName, directoryId);
    }

    /**
     * 删除子目录。
     *
     * @param subdirectory 指定待删除的目录。
     * @param recursive 指定是否递归删除子目录。
     * @return 如果删除成功返回 {@code true} 。
     */
    public boolean deleteDirectory(Directory subdirectory, boolean recursive) {
        return this.fileHierarchy.deleteDirectory(this, subdirectory, recursive);
    }

    /**
     * 删除多个子目录。
     *
     * @param subdirectories 指定待删除的目录列表。
     * @param recursive 指定是否递归删除子目录。
     * @return 返回被删除的目录，如果没有删除目录成功，返回 {@code null} 值。
     */
    public List<Directory> deleteDirectories(List<Directory> subdirectories, boolean recursive) {
        return this.fileHierarchy.deleteDirectories(this, subdirectories, recursive);
    }

    /**
     * 重命名。
     *
     * @param newName 指定新的目录名。
     * @return 返回目录。
     */
    public Directory renameDirectory(String newName) {
        return this.fileHierarchy.renameDirectory(this, newName);
    }

    /**
     * 设置是否为隐藏目录。
     *
     * @param hidden 指定是否隐藏。
     */
    public void setHidden(boolean hidden) {
        this.fileHierarchy.setHidden(this, hidden);
    }

    /**
     * 添加文件到目录。
     *
     * @param fileLabel 指定文件标签。
     * @return 添加成功返回 {@code true} 。
     */
    public boolean addFile(FileLabel fileLabel) {
        return this.fileHierarchy.addFileLabel(this, fileLabel);
    }

    /**
     * 从目录移除文件。
     *
     * @param fileLabel 指定文件标签。
     * @return 移除成功返回 {@code true} 。
     */
    public boolean removeFile(FileLabel fileLabel) {
        return this.fileHierarchy.removeFileLabel(this, fileLabel);
    }

    /**
     * 从目录移除文件。
     *
     * @param fileCodes 指定待移除的文件的文件码列表。
     * @return 返回移除的文件标签。
     */
    public List<FileLabel> removeFiles(List<String> fileCodes) {
        return this.fileHierarchy.removeFileLabelWithFileCodes(this, fileCodes);
    }

    /**
     * 查询指定索引范围内的所有文件标签实体。
     *
     * @param beginIndex 起始索引。
     * @param endIndex 结束索引。
     * @return 返回索引范围内的文件。
     */
    public List<FileLabel> listFiles(int beginIndex, int endIndex) {
        return this.fileHierarchy.listFileLabels(this, beginIndex, endIndex);
    }

    /**
     * 获取文件数量。
     *
     * @return 返回文件数量。
     */
    public int numFiles() {
        return this.fileHierarchy.numFiles(this);
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

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        if (null != this.fileHierarchy) {
            json.put("owner", this.fileHierarchy.getId());
        }

        json.put("id", this.getId().longValue());
        json.put("domain", this.getDomain().getName());
        json.put("name", this.getName());
        json.put("creation", this.getCreationTime());
        json.put("lastModified", this.getLastModified());
        json.put("size", this.getSize());
        json.put("hidden", this.isHidden());

        if (this.hasParent()) {
            json.put("parentId", this.getParent().getId().longValue());
        }

        // 子目录数量
        json.put("numDirs", this.numDirectories());
        // 文件数量
        json.put("numFiles", this.numFiles());

        JSONArray dirArray = new JSONArray();
        List<Directory> dirs = this.getDirectories();
        for (Directory dir : dirs) {
            dirArray.put(dir.toCompactJSON());
        }
        json.put("dirs", dirArray);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();

        if (null != this.fileHierarchy) {
            json.put("owner", this.fileHierarchy.getId());
        }

        json.put("id", this.getId().longValue());
        json.put("domain", this.getDomain().getName());
        json.put("name", this.getName());
        json.put("creation", this.getCreationTime());
        json.put("lastModified", this.getLastModified());
        json.put("size", this.getSize());
        json.put("hidden", this.isHidden());

        if (this.hasParent()) {
            json.put("parentId", this.getParent().getId().longValue());
        }

        // 子目录数量
        json.put("numDirs", this.numDirectories());
        // 文件数量
        json.put("numFiles", this.numFiles());
        
        return json;
    }
}
