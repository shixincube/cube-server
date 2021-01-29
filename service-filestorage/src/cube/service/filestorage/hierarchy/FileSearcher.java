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

        // 排序
        Collections.sort(this.fileQueue);
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
                index.incrementAndGet();
                
                if (result.size() >= num) {
                    return false;
                }

                if (filter.containsFileType(fileLabel.getFileType())) {

                }

                return true;
            }
        });

        if (end + 1 > result.size()) {
            end = result.size();
        }

        if (begin >= end) {
            return null;
        }

        return result;
    }

    /*
    public List<IndexingItem> listImageFile(final int beginIndex, final int endIndex) {
        if (this.fileQueue.size() < endIndex) {
            // 遍历
            FileHierarchyTool.recurseFile(this.root, new RecurseHandler() {
                @Override
                public boolean handle(Directory directory, FileLabel fileLabel) {
                    if (imageFiles.size() >= endIndex) {
                        return false;
                    }

                    return true;
                }
            });
        }

        if (beginIndex >= this.imageFiles.size()) {
            return new ArrayList<>();
        }

        List<IndexingItem> list = new ArrayList<>(this.imageFiles);

        int begin = beginIndex;
        int end = endIndex;

        if (end + 1 > list.size()) {
            end = list.size();
        }

        if (begin >= end) {
            return null;
        }

        list = list.subList(begin, end);
        return list;
    }*/

    protected static String makeKey(Directory directory, FileLabel fileLabel) {
        return directory.getId().toString() + fileLabel.getFileCode();
    }

    /**
     *
     */
    public class IndexingItem implements Comparable<IndexingItem> {

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
    }
}
