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

import cube.common.UniqueKey;
import cube.common.entity.FileLabel;
import cube.common.entity.HierarchyNode;
import cube.common.entity.HierarchyNodes;
import cube.service.filestorage.FileStructStorage;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件层级管理器。
 */
public class FileHierarchyManager implements FileHierarchyListener {

    /**
     * 所有联系人的基础文件层级。
     */
    private ConcurrentHashMap<String, FileHierarchy> roots;

    /**
     * 用于读写层级节点的缓存封装。
     */
    private FileHierarchyCache fileHierarchyCache;

    /**
     * 构造函数。
     *
     * @param structStorage
     */
    public FileHierarchyManager(FileStructStorage structStorage) {
        this.fileHierarchyCache = new FileHierarchyCache(structStorage);
        this.roots = new ConcurrentHashMap<>();
    }

    /**
     * 获取指定联系人的文件层级实例。
     *
     * @param contactId 指定联系人 ID 。
     * @param domainName 指定域名称。
     * @return 返回指定联系人的文件层级实例。
     */
    public synchronized FileHierarchy getFileHierarchy(Long contactId, String domainName) {
        String uniqueKey = UniqueKey.make(contactId, domainName);
        FileHierarchy root = this.roots.get(uniqueKey);
        if (null != root) {
            return root;
        }

        HierarchyNode node = HierarchyNodes.load(this.fileHierarchyCache, uniqueKey);
        if (null != node) {
            root = new FileHierarchy(this.fileHierarchyCache, node);
            this.roots.put(uniqueKey, root);
            return root;
        }

        // 没有创建过根目录，创建根目录
        node = new HierarchyNode(contactId, domainName);
        root = new FileHierarchy(this.fileHierarchyCache, node);
        this.roots.put(uniqueKey, root);

        // 设置监听器
        root.setListener(this);

        HierarchyNodes.save(this.fileHierarchyCache, node);

        return root;
    }

    public void onTick() {

    }

    @Override
    public void onFileLabelAdd(FileHierarchy fileHierarchy, Directory directory, FileLabel fileLabel) {
        // 更新容量
        long fileSize = fileLabel.getFileSize();

        Directory parent = directory.getParent();
        while (null != parent) {
            long size = parent.getSize();
            fileHierarchy.setDirectorySize(parent, size + fileSize);
            parent = parent.getParent();
        }
    }

    @Override
    public void onFileLabelRemove(FileHierarchy fileHierarchy, Directory directory, FileLabel fileLabel) {
        // 更新容量
        long fileSize = fileLabel.getFileSize();

        Directory parent = directory.getParent();
        while (null != parent) {
            long size = parent.getSize();
            fileHierarchy.setDirectorySize(parent, size - fileSize);
            parent = parent.getParent();
        }
    }

    /**
     * 仅用于辅助测试的方法。
     */
    public void clearMemory() {
        this.roots.clear();
    }
}
