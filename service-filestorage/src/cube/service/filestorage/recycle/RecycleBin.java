/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.filestorage.recycle;

import cell.util.log.Logger;
import cube.common.entity.FileLabel;
import cube.service.filestorage.ServiceStorage;
import cube.service.filestorage.hierarchy.Directory;
import cube.service.filestorage.hierarchy.FileHierarchyTool;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 文件回收站。
 */
public class RecycleBin {

    private ServiceStorage structStorage;

    /**
     * 构造函数。
     *
     * @param structStorage
     */
    public RecycleBin(ServiceStorage structStorage) {
        this.structStorage = structStorage;
    }

    /**
     * 读取指定根的废弃文件和目录。
     *
     * @param root
     * @param beginIndex
     * @param endIndex
     * @return
     */
    public List<Trash> get(Directory root, int beginIndex, int endIndex) {
        List<JSONObject> result = this.structStorage.listTrash(root.getDomain().getName(), root.getId(), beginIndex, endIndex);
        if (null == result) {
            return null;
        }

        List<Trash> list = new ArrayList<>(result.size());
        for (JSONObject json : result) {
            if (json.has("directory")) {
                DirectoryTrash trash = new DirectoryTrash(root, json);
                list.add(trash);
            }
            else {
                FileTrash trash = new FileTrash(root, json);
                list.add(trash);
            }
        }
        return list;
    }

    /**
     * 将指定目录放入回收站。
     *
     * @param root
     * @param directory
     */
    public void put(Directory root, Directory directory) {
        // 遍历目录结构
        LinkedList<Directory> list = new LinkedList<>();
        FileHierarchyTool.recurseParent(list, directory);

        // 创建回收链
        RecycleChain chain = new RecycleChain(list);

        // 废弃目录
        DirectoryTrash directoryTrash = new DirectoryTrash(root, chain, directory);

        this.structStorage.writeDirectoryTrash(directoryTrash);
    }

    /**
     * 将指定文件放入回收站。
     *
     * @param root
     * @param directory
     * @param fileLabel
     */
    public void put(Directory root, Directory directory, FileLabel fileLabel) {
        // TODO 相同文件码的记录管理
        // 目前的实现没有考虑相同文件码的文件，重复放入回收站是否记录每个文件副本。
        // 当相同的文件码放入回收站时，回收站目前依然记录，也就是说相同文件码的文件如果多次放入回收站，
        // 回收站每次都会记录，但是恢复的时候仅能恢复最后一次的数据。

        // 遍历目录结构
        LinkedList<Directory> list = new LinkedList<>();
        FileHierarchyTool.recurseParent(list, directory);

        // 创建回收链
        RecycleChain chain = new RecycleChain(list);

        // 废弃文件
        FileTrash fileTrash = new FileTrash(root, chain, fileLabel);

        this.structStorage.writeFileTrash(fileTrash);
    }

    /**
     * 将回收站内的废弃数据抹除，抹除后不可恢复。
     *
     * @param root
     * @param trashIdList
     */
    public void erase(Directory root, List<Long> trashIdList) {
        for (Long trashId : trashIdList) {
            this.structStorage.deleteTrash(root.getDomain().getName(), root.getId(), trashId);
        }
    }

    /**
     * 清空回收站。
     *
     * @param root
     */
    public void empty(Directory root) {
        this.structStorage.emptyTrash(root.getDomain().getName(), root.getId());
    }

    /**
     * 恢复文件或文件夹
     *
     * @param root
     * @param trashIdList
     * @return
     */
    public RestoreResult restore(Directory root, List<Long> trashIdList) {
        RestoreResult result = new RestoreResult();

        for (Long trashId : trashIdList) {
            JSONObject json = this.structStorage.readTrash(root.getDomain().getName(), root.getId(), trashId);
            if (null == json) {
                Logger.w(this.getClass(), "Can NOT find trash data: " + trashId);
                continue;
            }

            if (json.has("file")) {
                FileTrash fileTrash = new FileTrash(root, json);
                if (restore(fileTrash)) {
                    result.successList.add(fileTrash);
                    this.structStorage.deleteTrash(root.getDomain().getName(), root.getId(), trashId);
                }
                else {
                    result.failureList.add(fileTrash);
                }
            }
            else if (json.has("directory")) {
                DirectoryTrash directoryTrash = new DirectoryTrash(root, json);
                if (restore(directoryTrash)) {
                    result.successList.add(directoryTrash);
                    this.structStorage.deleteTrash(root.getDomain().getName(), root.getId(), trashId);
                }
                else {
                    result.failureList.add(directoryTrash);
                }
            }
        }

        return result;
    }

    /**
     * 恢复文件。
     *
     * @param fileTrash
     * @return
     */
    private boolean restore(FileTrash fileTrash) {
        Directory root = fileTrash.getRoot();
        Directory parent = root;

        RecycleChain chain = fileTrash.getChain();
        for (Directory dir : chain.getNodes()) {
            if (dir.getName().equals("root")) {
                continue;
            }

            if (!parent.existsDirectory(dir.getName())) {
                Directory newDir = parent.createDirectory(dir.getName());
                parent = newDir;
            }
            else {
                parent = parent.getDirectory(dir.getName());
            }
        }

        // 恢复文件到目录
        boolean success = parent.addFile(fileTrash.getFileLabel());
        return success;
    }

    /**
     * 恢复目录。
     *
     * @param directoryTrash
     * @return
     */
    private boolean restore(DirectoryTrash directoryTrash) {
        Directory root = directoryTrash.getRoot();
        RecycleChain chain = directoryTrash.getChain();

        Directory current = root;
        List<Directory> list = chain.getNodes();
        for (int i = 1; i < list.size(); ++i) {
            Directory dir = list.get(i);

            if (!current.existsDirectory(dir.getName())) {
                Directory newDir = current.createDirectory(dir.getName(), dir.getId());
                current = newDir;
            }
            else {
                current = current.getDirectory(dir.getName());
            }

            if (dir.getId().longValue() == directoryTrash.getOriginalId().longValue()) {
                // 遍历到自己，结束
                break;
            }
        }

        if (null == current) {
            return false;
        }

        int total = directoryTrash.getDirectory().numFiles();
        List<FileLabel> files = directoryTrash.getDirectory().listFiles(0, total);
        for (FileLabel file : files) {
            current.addFile(file);
        }

        return true;
    }
}
