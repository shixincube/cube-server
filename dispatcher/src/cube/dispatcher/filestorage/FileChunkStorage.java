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

package cube.dispatcher.filestorage;

import cell.util.CachedQueueExecutor;
import cube.util.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * 文件块存储器。
 */
public class FileChunkStorage {

    private Path workingPath;

    private ConcurrentHashMap<String, FileChunkStore> fileChunks;

    private ExecutorService executor;

    public FileChunkStorage(String path) {
        this.workingPath = Paths.get(path).toAbsolutePath();
        if (!Files.exists(this.workingPath)) {
            try {
                Files.createDirectories(this.workingPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.fileChunks = new ConcurrentHashMap<>();
    }

    public void open() {
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(4);
    }

    public void close() {
        this.executor.shutdown();
    }

    public String append(FileChunk chunk) {
        String fileCode = FileUtils.makeFileCode(chunk.contactId, chunk.domain, chunk.fileName);

        FileChunkStore store = this.fileChunks.get(fileCode);
        if (null == store) {
            store = new FileChunkStore(fileCode);
            this.fileChunks.put(fileCode, store);
        }

        store.add(chunk);

        this.executor.execute(store);

        return fileCode;
    }

    public void writeToDisk(FileChunkStore fileChunkStore) {
        if (!fileChunkStore.completed) {
            return;
        }

        File file = new File(this.workingPath.toString(), fileChunkStore.fileCode);
        if (file.exists()) {
            file.delete();
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            for (FileChunk chunk : fileChunkStore.chunks) {
                fos.write(chunk.data);
            }
            fos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }


    /**
     * 文件块列表。
     */
    protected class FileChunkStore implements Runnable {

        protected String fileCode;

        protected ArrayList<FileChunk> chunks;

        protected boolean completed = false;

        protected FileChunkStore(String fileCode) {
            this.fileCode = fileCode;
            this.chunks = new ArrayList<>();
        }

        protected void add(FileChunk fileChunk) {
            synchronized (this.chunks) {
                int index = this.chunks.indexOf(fileChunk);
                if (index >= 0) {
                    this.chunks.remove(index);
                }

                this.chunks.add(fileChunk);
            }
        }

        @Override
        public void run() {
            // 自检

            // 排序
            Collections.sort(this.chunks);

            // 检查块是否连续
            boolean continuous = true;
            boolean completed = false;

            synchronized (this.chunks) {
                for (int i = 0, ni = 1, size = this.chunks.size(); i < size; ++i, ++ni) {
                    FileChunk chunk = this.chunks.get(i);
                    FileChunk next = ni < size ? this.chunks.get(ni) : null;

                    if (null != next) {
                        if (chunk.cursor + chunk.size != next.cursor) {
                            continuous = false;
                            break;
                        }
                    }
                    else {
                        // next 为空，chunk 是最后一块，判断是否结束
                        if (chunk.cursor + chunk.size == chunk.fileSize) {
                            completed = true;
                        }
                    }
                }
            }

            if (continuous && completed) {
                this.completed = true;
                writeToDisk(this);
            }
        }
    }
}
