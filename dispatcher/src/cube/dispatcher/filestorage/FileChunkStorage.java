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

package cube.dispatcher.filestorage;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.CachedQueueExecutor;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.action.FileStorageAction;
import cube.common.entity.FileLabel;
import cube.dispatcher.DispatcherTask;
import cube.dispatcher.Performer;
import cube.util.FileType;
import cube.util.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 文件块存储器。
 */
public class FileChunkStorage {

    private FileStorageCellet cellet;

    private Performer performer;

    private Path workingPath;

    private ConcurrentSkipListSet<FileChunkTag> chunkTags;

    /**
     * 文件码对应的文件块存储。
     */
    private ConcurrentHashMap<String, FileChunkStore> fileChunkStores;

    /**
     * 正在进行透传的区块。
     */
    private ConcurrentHashMap<String, ChunkInputStream> passingChunkInputStreams;

    /**
     * 线程池。
     */
    private ExecutorService executor;

    /**
     * 是否将文件写到磁盘。
     */
    private boolean writeDisk = false;

    /**
     * 为每一个文件分配的内存缓存大小，超过该大小的部分将写入临时文件。
     */
    private int sizeEachFile = 5 * 1024 * 1024;

    public FileChunkStorage(String path) {
        this.workingPath = Paths.get(path).toAbsolutePath();
        if (!Files.exists(this.workingPath)) {
            try {
                Files.createDirectories(this.workingPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.chunkTags = new ConcurrentSkipListSet<>();
        this.fileChunkStores = new ConcurrentHashMap<>();
        this.passingChunkInputStreams = new ConcurrentHashMap<>();
    }

    public void open(FileStorageCellet cellet, Performer performer) {
        this.cellet = cellet;
        this.performer = performer;
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(16);
    }

    public void close() {
        this.executor.shutdown();
    }

    /**
     * 添加文件数据块。
     *
     * @param chunk
     * @return
     */
    public String append(FileChunk chunk) {
        // 匹配文件码
        String fileCode = this.matchFileCode(chunk);

        FileChunkStore store = null;

        synchronized (this) {
            store = this.fileChunkStores.get(fileCode);
            if (null == store) {
                store = new FileChunkStore(fileCode, chunk.token);
                this.fileChunkStores.put(fileCode, store);
            }

            store.add(chunk);
        }

        if (!store.running.get()) {
            store.running.set(true);
            this.executor.execute(store);
        }

        return fileCode;
    }

    /**
     * 关闭指定文件码的文件。
     *
     * @param fileCode
     */
    public void closeFile(String fileCode) {
        this.passingChunkInputStreams.remove(fileCode);

        FileChunkStore store = this.fileChunkStores.remove(fileCode);
        if (null != store) {
            store.close();
        }
    }

    /**
     * 将文件写到磁盘。
     *
     * @param fileChunkStore
     */
    public void writeToDisk(FileChunkStore fileChunkStore) {
        if (!this.writeDisk) {
            // 不写入磁盘
           return;
        }

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
                fos.write(chunk.getData());
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
        if (this.passingChunkInputStreams.containsKey(fileChunkStore.fileCode)) {
            return;
        }

        ChunkInputStream inputStream = new ChunkInputStream(fileChunkStore);
        this.passingChunkInputStreams.put(fileChunkStore.fileCode, inputStream);

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                // 将文件流发给服务单元
                performer.transmit(fileChunkStore.tokenCode, FileStorageCellet.NAME, fileChunkStore.fileCode, inputStream);

                // 将文件流进行标记
                markStream(fileChunkStore);

                // 清理内存
                closeFile(fileChunkStore.fileCode);
            }
        });
    }

    /**
     * 标记传送给服务单元的文件流。
     *
     * @param fileChunkStore
     */
    private void markStream(final FileChunkStore fileChunkStore) {
        // 生成文件标签
        FileLabel fileLabel = fileChunkStore.makeFileLabel();

        // 创建包
        Packet packet = new Packet(FileStorageAction.PutFile.name, fileLabel.toJSON());

        // 发送
        ActionDialect dialect = this.performer.syncTransmit(fileChunkStore.tokenCode, FileStorageCellet.NAME, packet.toDialect());
        if (null == dialect) {
            return;
        }

        // 将数据发送给客户端
        TalkContext talkContext = this.performer.getTalkContext(fileChunkStore.tokenCode);
        if (null == talkContext) {
            Logger.w(this.getClass(), "Can NOT find talk context: " + fileChunkStore.tokenCode);
            return;
        }

        // 返回给客户端，需要追加状态
        this.cellet.speak(talkContext, DispatcherTask.appendState(dialect));
    }

    /**
     * 匹配文件区块对应的文件码。
     *
     * @param chunk
     * @return
     */
    private String matchFileCode(FileChunk chunk) {
        FileChunkTag current = null;

        for (FileChunkTag tag : this.chunkTags) {
            if (tag.contactId == chunk.contactId && tag.domain.equals(chunk.domain) &&
                tag.fileName.equals(chunk.fileName)) {
                current = tag;
                break;
            }
        }

        if (null != current) {
            return current.fileCode;
        }

        // 生成文件码
        String fileCode = FileUtils.makeFileCode(chunk.contactId, chunk.domain, chunk.fileName);
        current = new FileChunkTag(chunk, fileCode);
        this.chunkTags.add(current);
        return current.fileCode;
    }

    private void removeChunkTag(String fileCode) {
        for (FileChunkTag tag : this.chunkTags) {
            if (tag.fileCode.equals(fileCode)) {
                this.chunkTags.remove(tag);
                break;
            }
        }
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

        private long totalLength = 0;

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

                // 更新总大小
                this.totalLength += fileChunk.size;

                if (this.totalLength > sizeEachFile) {
                    // 超过缓存阀值
                    fileChunk.flush(workingPath, fileCode + "." + fileChunk.cursor + ".tmp");
                }
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

        protected void close() {
            // 移除 Chunk Tag
            removeChunkTag(this.fileCode);

            synchronized (this.chunks) {
                for (FileChunk chunk : this.chunks) {
                    chunk.clear();
                }

                this.chunks.clear();
            }
        }

        /**
         * 生成文件标签。
         *
         * @return
         */
        protected FileLabel makeFileLabel() {
            // 计算文件散列码
            MessageDigest md5 = null;
            MessageDigest sha1 = null;
            try {
                md5 = MessageDigest.getInstance("MD5");
                sha1 = MessageDigest.getInstance("SHA1");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            synchronized (this.chunks) {
                // 排序
                Collections.sort(this.chunks);

                for (FileChunk chunk : this.chunks) {
                    byte[] data = chunk.getData();
                    md5.update(data);
                    sha1.update(data);
                }
            }

            byte[] hashMD5 = md5.digest();
            byte[] hashSHA1 = sha1.digest();
            String md5Code = FileUtils.bytesToHexString(hashMD5);
            String sha1Code = FileUtils.bytesToHexString(hashSHA1);

            FileChunk chunk = this.chunks.get(0);

            // 判断文件类型
            FileType fileType = FileUtils.verifyFileType(chunk.fileName, chunk.getData());

            FileLabel fileLabel = new FileLabel(chunk.domain, this.fileCode, chunk.contactId, chunk.fileName,
                    chunk.fileSize, chunk.lastModified, System.currentTimeMillis(), 0);
            fileLabel.setFileType(fileType);
            fileLabel.setMD5Code(md5Code);
            fileLabel.setSHA1Code(sha1Code);

            return fileLabel;
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

            if (this.chunks.get(0).cursor == 0) {
                // 将文件传输给服务节点
                expressToService(this);
            }

            if (continuous && completed) {
                this.completed = true;
                // 完成写入磁盘
                writeToDisk(this);
            }

            this.running.set(false);
        }
    }

    /**
     * 文件块输入流。
     */
    protected class ChunkInputStream extends InputStream {

        private FileChunkStore store;

        private int cursor = -1;

        private int chunkCursor = -1;

        private FileChunk current = null;

        private byte[] currentData = null;

        public ChunkInputStream(FileChunkStore store) {
            this.store = store;
            this.current = store.get(0);
            this.currentData = this.current.getData();
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

            if (this.chunkCursor >= this.current.size || this.chunkCursor >= this.currentData.length) {
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
                    this.currentData = chunk.getData();
                }
                else {
                    this.current = chunk;
                    this.currentData = chunk.getData();
                }
            }

            byte b = this.currentData[this.chunkCursor];
            return b & 0xFF;
        }

        @Override
        public void close() throws IOException {
            this.current = null;
            this.currentData = null;
            passingChunkInputStreams.remove(this.store.fileCode);
        }
    }
}
