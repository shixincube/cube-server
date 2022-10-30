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

import cell.util.log.Logger;
import cube.common.UniqueKey;
import cube.common.entity.FileLabel;
import cube.common.entity.HierarchyNode;
import cube.common.entity.HierarchyNodes;
import cube.service.filestorage.FileStorageService;
import cube.service.filestorage.ServiceStorage;
import cube.util.FileUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件层级管理器。
 */
public class FileHierarchyManager implements FileHierarchyListener {

    /**
     * 对象的生命周期。
     */
    private long fileHierarchyDuration = 24L * 60 * 60 * 1000;

    private long spaceSizeCheckDuration = 12L * 60 * 60 * 1000;

    /**
     * 所有联系人的基础文件层级。
     */
    private ConcurrentHashMap<String, FileHierarchy> roots;

    /**
     * 用于读写层级节点的缓存封装。
     */
    private FileHierarchyCache fileHierarchyCache;

    /**
     * 文件存储服务。
     */
    private FileStorageService fileStorageService;

    /**
     * 构造函数。
     *
     * @param structStorage
     */
    public FileHierarchyManager(ServiceStorage structStorage, FileStorageService fileStorageService) {
        this.fileHierarchyCache = new FileHierarchyCache(structStorage);
        this.fileStorageService = fileStorageService;
        this.roots = new ConcurrentHashMap<>();
    }

    /**
     * 获取指定 ROOT 的文件层级实例。
     *
     * @param rootId 指定联系人 ID 。
     * @param domainName 指定域名称。
     * @return 返回指定联系人的文件层级实例。
     */
    public synchronized FileHierarchy getFileHierarchy(Long rootId, String domainName) {
        String uniqueKey = UniqueKey.make(rootId, domainName);
        FileHierarchy root = this.roots.get(uniqueKey);
        if (null != root) {
            return root;
        }

        HierarchyNode node = HierarchyNodes.load(this.fileHierarchyCache, uniqueKey);
        if (null != node) {
            root = new FileHierarchy(this.fileHierarchyCache, node, this);
            this.roots.put(uniqueKey, root);
            return root;
        }

        // 没有创建过根目录，创建根目录
        node = new HierarchyNode(rootId, domainName);
        root = new FileHierarchy(this.fileHierarchyCache, node, this);
        this.roots.put(uniqueKey, root);

        HierarchyNodes.save(this.fileHierarchyCache, node);

        return root;
    }

    /**
     * 计算文件总数量。
     *
     * @param domainName
     * @param contactIdOrGroupId
     * @return
     */
    public long countFileTotalSize(String domainName, long contactIdOrGroupId) {
        FileHierarchy fileHierarchy = this.getFileHierarchy(contactIdOrGroupId, domainName);
        return fileHierarchy.getRoot().getSize();
    }

    

    /**
     * 周期回调。用于维护过期的数据。
     */
    public void onTick() {
        long now = System.currentTimeMillis();

        List<FileHierarchy> needCheckList = new ArrayList<>();

        Iterator<FileHierarchy> fhiter = this.roots.values().iterator();
        while (fhiter.hasNext()) {
            FileHierarchy hierarchy = fhiter.next();

            // 矫正容量
            if (now - hierarchy.getRoot().getLastModified() > this.spaceSizeCheckDuration) {
                needCheckList.add(hierarchy);
            }

            // 容量校验
            if (hierarchy.getRoot().getSize() > hierarchy.getCapacity()) {
                // 容量越界
                Logger.w(this.getClass(), "File hierarchy capacity overflow ： " + hierarchy.getId()
                        + " - " + FileUtils.scaleFileSize(hierarchy.getRoot().getSize())
                        + "/" + FileUtils.scaleFileSize(hierarchy.getCapacity()));
            }

            if (now - hierarchy.getTimestamp() > this.fileHierarchyDuration) {
                // 到期，从内存里移除
                fhiter.remove();
            }
        }

        if (!needCheckList.isEmpty()) {
            if (Logger.isDebugLevel()) {
                Logger.d(FileHierarchyManager.class, "Check file hierarchy size (" + needCheckList.size() + ")");
            }

            (new Thread() {
                @Override
                public void run() {
                    while (!needCheckList.isEmpty()) {
                        FileHierarchy hierarchy = needCheckList.remove(0);

                        // 矫正目录里的文件总大小
                        FileHierarchyTool.recurseDirectory(hierarchy.getRoot(), new RecurseDirectoryHandler() {
                            @Override
                            public boolean handle(Directory directory) {
                                if (directory.isHidden()) {
                                    return true;
                                }

                                // 没有子目录的目录
                                long size = 0;
                                int begin = 0;
                                int end = 49;
                                List<FileLabel> list = directory.listFiles(begin, end);
                                while (!list.isEmpty()) {
                                    for (FileLabel fileLabel : list) {
                                        size += fileLabel.getFileSize();
                                    }

                                    begin = end;
                                    end += 50;
                                    list = directory.listFiles(begin, end);
                                }

                                // 重置文件总大小
                                hierarchy.setFileTotalSize(directory, size);

                                // 重置当前目录的大小
                                if (0 == directory.numDirectories()) {
                                    // 叶子节点设置目录大小
                                    hierarchy.setDirectorySize(directory, size);
                                }
                                else {
                                    // 非叶子节点仅更新内存里的数据，以便后续计算
                                    directory.setSize(size);
                                }

                                return true;
                            }
                        });

                        // 矫正总大小，从叶子向上遍历
                        FileHierarchyTool.recurseDirectory(hierarchy.getRoot(), new RecurseDirectoryHandler() {
                            @Override
                            public boolean handle(Directory directory) {
                                if (directory.isHidden()) {
                                    return true;
                                }

                                if (0 == directory.numDirectories()) {
                                    long dirSize = directory.getSize();
                                    Directory parent = directory.getParent();
                                    while (null != parent) {
                                        long size = parent.getSize();
                                        hierarchy.setDirectorySize(parent, size + dirSize);
                                        parent = parent.getParent();
                                    }
                                }
                                return true;
                            }
                        });
                    }
                }
            }).start();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDirectoryRemove(FileHierarchy fileHierarchy, List<Directory> directories) {
        for (Directory directory : directories) {
            // 将数据放入回收站
            this.fileStorageService.getRecycleBin().put(fileHierarchy.getRoot(), directory);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFileLabelAdd(FileHierarchy fileHierarchy, Directory directory, FileLabel fileLabel) {
        if (directory.isHidden()) {
            // 隐藏目录不计算大小
            return;
        }

        // 更新容量
        long fileSize = fileLabel.getFileSize();

        Directory parent = directory.getParent();
        while (null != parent) {
            long size = parent.getSize();
            fileHierarchy.setDirectorySize(parent, size + fileSize);
            parent = parent.getParent();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFileLabelRemove(FileHierarchy fileHierarchy, Directory directory, List<FileLabel> fileLabels) {
        // 更新容量
        long fileSize = 0;
        for (FileLabel fileLabel : fileLabels) {
            fileSize += fileLabel.getFileSize();
            // 将数据放入回收站
            this.fileStorageService.getRecycleBin().put(fileHierarchy.getRoot(), directory, fileLabel);
        }

        if (directory.isHidden()) {
            // 隐藏目录不影响父目录空间计算
            return;
        }

        Directory parent = directory.getParent();
        while (null != parent) {
            long size = parent.getSize();
            fileHierarchy.setDirectorySize(parent, size - fileSize);
            parent = parent.getParent();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileLabel onQueryFileLabel(FileHierarchy fileHierarchy, Directory directory, String uniqueKey) {
        return this.fileStorageService.getFile(directory.getDomain().getName(), uniqueKey);
    }

    /**
     * 仅用于辅助测试的方法。
     */
    public void clearMemory() {
        this.roots.clear();
    }
}
