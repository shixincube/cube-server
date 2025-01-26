/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.filestorage.hierarchy;

import cube.common.entity.FileLabel;

import java.util.List;

/**
 * 文件结构监听器。
 */
public interface FileHierarchyListener {

    /**
     * 当目录删除时调用该函数。
     *
     * @param fileHierarchy 文件层级。
     * @param directories 删除的目录列表。
     */
    public void onDirectoryRemove(FileHierarchy fileHierarchy, List<Directory> directories);

    /**
     * 当添加文件标签时调用该函数。
     *
     * @param fileHierarchy 文件层级。
     * @param directory 文件目录。
     * @param fileLabel 文件标签。
     */
    public void onFileLabelAdd(FileHierarchy fileHierarchy, Directory directory, FileLabel fileLabel);

    /**
     * 当移除文件标签时调用该函数。
     *
     * @param fileHierarchy 文件层级。
     * @param directory 文件目录。
     * @param fileLabels 文件标签列表。
     */
    public void onFileLabelRemove(FileHierarchy fileHierarchy, Directory directory, List<FileLabel> fileLabels);

    /**
     * 查询指定的文件标签。
     *
     * @param fileHierarchy 文件层级。
     * @param directory 文件目录。
     * @param fileCode 文件标签的文件码。
     * @return 返回对应的文件标签。
     */
    public FileLabel onQueryFileLabel(FileHierarchy fileHierarchy, Directory directory, String fileCode);
}
