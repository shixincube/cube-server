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

package cube.service.filestorage.recycle;

import cube.common.entity.FileLabel;
import cube.service.filestorage.FileStructStorage;
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

    private FileStructStorage structStorage;

    /**
     * 构造函数。
     *
     * @param structStorage
     */
    public RecycleBin(FileStructStorage structStorage) {
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

    public void recover(Directory root, Long directoryId) {

    }
}
