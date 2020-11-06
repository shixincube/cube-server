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
import cell.util.log.Logger;
import cube.common.Packet;
import cube.dispatcher.Performer;
import cube.util.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 文件块存储器。
 */
public class FileChunkStorage {

    private final int blockSize = 128 * 1024;

    private Performer performer;

    private Path workingPath;

    private ConcurrentHashMap<String, FileChunkStore> fileChunks;

    /**
     * 正在进行透传的区块。
     */
    private ConcurrentHashMap<String, ChunkInputStream> passingChunks;

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
        this.passingChunks = new ConcurrentHashMap<>();
    }

    public void open(Performer performer) {
        this.performer = performer;
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(16);
    }

    public void close() {
        this.executor.shutdown();
    }

    public String append(FileChunk chunk) {
        String fileCode = FileUtils.makeFileCode(chunk.contactId, chunk.domain, chunk.fileName);

        FileChunkStore store = this.fileChunks.get(fileCode);
        if (null == store) {
            store = new FileChunkStore(fileCode, chunk.token);
            this.fileChunks.put(fileCode, store);
        }

        store.add(chunk);

        if (!store.running.get()) {
            store.running.set(true);
            this.executor.execute(store);
        }

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
     * 将收到的数据实时转发给服务节点。
     *
     * @param fileChunkStore
     */
    public synchronized void expressToService(final FileChunkStore fileChunkStore) {
        if (this.passingChunks.containsKey(fileChunkStore.fileCode)) {
            return;
        }

        ChunkInputStream inputStream = new ChunkInputStream(fileChunkStore);
        this.passingChunks.put(fileChunkStore.fileCode, inputStream);

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                performer.transmit(fileChunkStore.tokenCode, FileStorageCellet.NAME, fileChunkStore.fileCode, inputStream);
            }
        });
    }


    /**
     * 文件块列表。
     */
    protected class FileChunkStore implements Runnable {

        protected String fileCode;

        protected String tokenCode;

        private ArrayList<FileChunk> chunks;

        protected boolean completed = false;

        protected AtomicBoolean running = new AtomicBoolean(false);

        protected FileChunkStore(String fileCode, String tokenCode) {
            this.fileCode = fileCode;
            this.tokenCode = tokenCode;
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

        protected FileChunk last() {
            synchronized (this.chunks) {
                return this.chunks.get(this.chunks.size() - 1);
            }
        }

        protected int numChunks() {
            synchronized (this.chunks) {
                return this.chunks.size();
            }
        }

        protected FileChunk get(long cursor) {
            synchronized (this.chunks) {
                for (int i = 0, size = this.chunks.size(); i < size; ++i) {
                    FileChunk fileChunk = this.chunks.get(i);
                    if (fileChunk.cursor == cursor) {
                        return fileChunk;
                    }
                }
            }

            return null;
        }


        @Override
        public void run() {
            // 检查块是否连续
            boolean continuous = true;
            boolean completed = false;

            synchronized (this.chunks) {
                // 排序
                Collections.sort(this.chunks);

                // 检查状态
                for (int i = 0, ni = 1, size = this.chunks.size(); i < size; ++i, ++ni) {
                    FileChunk chunk = this.chunks.get(i);
                    FileChunk next = ni < size ? this.chunks.get(ni) : null;

                    if (null != next) {
                        if (chunk.position != next.cursor) {
                            continuous = false;
                            break;
                        }
                    }
                    else {
                        // next 为空，chunk 是最后一块，判断是否结束
                        if (chunk.position == chunk.fileSize) {
                            if (this.chunks.get(0).cursor == 0) {
                                completed = true;
                            }
                        }
                    }
                }
            }

            if (continuous && completed) {
                this.completed = true;
                // 完成写入磁盘
                writeToDisk(this);
            }
            else if (this.chunks.get(0).cursor == 0) {
                expressToService(this);
            }

            this.running.set(false);
        }
    }

    protected class ChunkInputStream extends InputStream {

        private FileChunkStore store;

        private int cursor = -1;

        private int chunkCursor = -1;

        private FileChunk current = null;

        public ChunkInputStream(FileChunkStore store) {
            this.store = store;
            this.current = store.get(0);
        }

        @Override
        public int read() throws IOException {
            // 文件游标后移
            ++this.cursor;
            // 块游标后移
            ++this.chunkCursor;

            if (this.store.completed && this.chunkCursor >= this.current.size && this.current.position >= this.current.fileSize) {
                // 已经完成读取
                return -1;
            }

            if (this.chunkCursor >= this.current.size) {
                // 重置块游标
                this.chunkCursor = 0;

                if (this.store.completed && this.current.position >= this.current.fileSize) {
                    // 已经完成读取
                    return -1;
                }

                FileChunk chunk = this.store.get(this.current.position);
                if (null == chunk) {
                    // 没有完成，但是无新数据，进行等待
                    int waitCount = 3000;
                    while (null == chunk) {
                        // 等待
                        try {
                            Thread.sleep(100L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        --waitCount;
                        if (waitCount < 0) {
                            break;
                        }

                        chunk = this.store.get(this.current.position);
                    }

                    if (null == chunk) {
                        // 超时结束
                        Logger.w(this.getClass(), "Chunk file stream timeout: " + this.store.fileCode);
                        return -1;
                    }

                    this.current = chunk;
                }
                else {
                    this.current = chunk;
                }
            }

            byte b = this.current.data[this.chunkCursor];
            return b & 0xFF;
        }
    }
}
