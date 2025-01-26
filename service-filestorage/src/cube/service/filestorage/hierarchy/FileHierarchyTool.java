/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.filestorage.hierarchy;

import cube.common.entity.FileLabel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

/**
 * 文件层级系统辅助工具。
 */
public final class FileHierarchyTool {

    private FileHierarchyTool() {
    }

    /**
     * 从指定目录开始递归所有父目录。
     *
     * @param list
     * @param directory
     */
    public static void recurseParent(LinkedList<Directory> list, Directory directory) {
        list.addFirst(directory);

        Directory parent = directory.getParent();
        if (null == parent) {
            return;
        }

        FileHierarchyTool.recurseParent(list, parent);
    }

    /**
     * 打包目录所有的数据到 JSON 描述的数据里。
     *
     * @param directory
     * @param json
     */
    public static void packDirectory(Directory directory, JSONObject json) {
        Directory parent = directory.getParent();
        if (null != parent) {
            json.put("parent", parent.toCompactJSON());
        }

        JSONArray files = new JSONArray();
        int numFiles = directory.numFiles();
        List<FileLabel> fileList = directory.listFiles(0, numFiles);
        for (FileLabel fl : fileList) {
            files.put(fl.toCompactJSON());
        }
        json.put("files", files);

        JSONArray dirs = new JSONArray();
        List<Directory> directoryList = directory.getDirectories();
        for (Directory dir : directoryList) {
            JSONObject dirJson = dir.toCompactJSON();
            FileHierarchyTool.packDirectory(dir, dirJson);
            dirs.put(dirJson);
        }
        json.put("dirs", dirs);
    }

    /**
     * 解包数据被打包的目录数据结构。
     *
     * @param json
     * @return
     */
    public static MetaDirectory unpackDirectory(JSONObject json) {
        MetaDirectory directory = new MetaDirectory(json);

        if (json.has("parent")) {
            directory.setParent(new MetaDirectory(json.getJSONObject("parent")));
        }

        JSONArray files = json.getJSONArray("files");
        for (int i = 0; i < files.length(); ++i) {
            JSONObject fileJson = files.getJSONObject(i);
            FileLabel fileLabel = new FileLabel(fileJson);
            directory.addFile(fileLabel);
        }

        JSONArray dirs = json.getJSONArray("dirs");
        for (int i = 0; i < dirs.length(); ++i) {
            JSONObject dirJson = dirs.getJSONObject(i);
            MetaDirectory dir = FileHierarchyTool.unpackDirectory(dirJson);
            directory.addChild(dir);
        }

        return directory;
    }

    /**
     * 递归文件。
     *
     * @param directory
     * @param handler
     * @return
     */
    public static boolean recurseFile(Directory directory, RecurseFileHandler handler) {
        List<FileLabel> list = directory.listFiles(0, directory.numFiles());
        for (FileLabel fileLabel : list) {
            if (!handler.handle(directory, fileLabel)) {
                return false;
            }
        }

        List<Directory> subdirs = directory.getDirectories();
        for (Directory subdir : subdirs) {
            if (!FileHierarchyTool.recurseFile(subdir, handler)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 递归目录。
     *
     * @param directory
     * @param handler
     * @return
     */
    public static boolean recurseDirectory(Directory directory, RecurseDirectoryHandler handler) {
        List<Directory> directories = directory.getDirectories();
        for (Directory dir : directories) {
            if (!handler.handle(dir)) {
                return false;
            }

            if (!FileHierarchyTool.recurseDirectory(dir, handler)) {
                return false;
            }
        }

        return true;
    }
}
