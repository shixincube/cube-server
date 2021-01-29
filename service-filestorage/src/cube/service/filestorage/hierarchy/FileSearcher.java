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

import cube.common.JSONable;
import cube.common.entity.FileLabel;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 文件分类器。
 */
public class FileSearcher {

    private Directory root;

    private FileHierarchyListener listener;

    private LinkedHashMap<String, IndexingItem> repository;

    private Vector<IndexingItem> fileQueue;

    private ConcurrentHashMap<SearchFilter, List<IndexingItem>> searchResultMap;

    public FileSearcher(Directory root, FileHierarchyListener listener) {
        this.root = root;
        this.listener = listener;
        this.repository = new LinkedHashMap<>();
        this.fileQueue = new Vector<>();
        this.searchResultMap = new ConcurrentHashMap<>();
    }

    public void addFile(Directory directory, FileLabel fileLabel) {
        String key = FileSearcher.makeKey(directory, fileLabel);
        if (this.repository.containsKey(key)) {
            return;
        }

        IndexingItem item = new IndexingItem(key, directory, fileLabel);
        this.repository.put(key, item);
        this.fileQueue.add(item);
    }

    public void removeFile(Directory directory, FileLabel fileLabel) {
        String key = FileSearcher.makeKey(directory, fileLabel);
        IndexingItem item = this.repository.remove(key);
        if (null != item) {
            this.fileQueue.remove(item);
        }
    }

    /**
     * 搜索指定规则的文件。
     *
     * @param filter
     * @return
     */
    public List<IndexingItem> search(SearchFilter filter) {
        if (filter.beginIndex >= filter.endIndex) {
            return null;
        }

        List<IndexingItem> record = this.searchResultMap.get(filter);
        if (null != record) {
            return record;
        }

        List<IndexingItem> result = new ArrayList<>();

        int begin = filter.beginIndex;
        int end = filter.endIndex;
        final int num = end - begin;

        // 从根目录开始进行递归
        AtomicInteger index = new AtomicInteger(0);
        FileHierarchyTool.recurseFile(this.root, new RecurseHandler() {
            @Override
            public boolean handle(Directory directory, FileLabel fileLabel) {
                if (result.size() >= num) {
                    return false;
                }

                if (filter.containsFileType(fileLabel.getFileType())) {
                    // 文件类型匹配
                    int curIndex = index.get();
                    if (curIndex >= begin && curIndex <= end) {
                        result.add(new IndexingItem(FileSearcher.makeKey(directory, fileLabel),
                                directory, fileLabel));
                    }

                    // 更新索引
                    index.incrementAndGet();
                }

                return true;
            }
        });

        // 排序
        Collections.sort(result);

        // 缓存记录
        this.searchResultMap.put(filter, result);

        return result;
    }

    protected static String makeKey(Directory directory, FileLabel fileLabel) {
        return directory.getId().toString() + fileLabel.getFileCode();
    }

    /**
     *
     */
    public class IndexingItem implements Comparable<IndexingItem>, JSONable {

        protected String key;

        protected Directory directory;

        protected FileLabel fileLabel;

        protected IndexingItem(String key, Directory directory, FileLabel fileLabel) {
            this.key = key;
            this.directory = directory;
            this.fileLabel = fileLabel;
        }

        @Override
        public int compareTo(IndexingItem o) {
            // 时间倒序
            if (this.fileLabel.getLastModified() < o.fileLabel.getLastModified()) {
                return 1;
            }
            else if (this.fileLabel.getLastModified() > o.fileLabel.getLastModified()) {
                return -1;
            }
            else {
                return 0;
            }
        }

        @Override
        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("directory", this.directory.toCompactJSON());
            json.put("file", this.fileLabel.toCompactJSON());
            return json;
        }

        @Override
        public JSONObject toCompactJSON() {
            return this.toJSON();
        }
    }
}
