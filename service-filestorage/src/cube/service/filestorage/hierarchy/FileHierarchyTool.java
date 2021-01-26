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

    public static void recurse(LinkedList<Directory> list, Directory directory) {
        list.addFirst(directory);

        Directory parent = directory.getParent();
        if (null == parent) {
            return;
        }

        FileHierarchyTool.recurse(list, parent);
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
}
