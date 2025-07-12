/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc;

import cell.util.log.Logger;
import cube.aigc.psychology.Painting;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CacheCenter {

    private final static CacheCenter instance = new CacheCenter();

    private final File cachePath = new File("cache/");

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final int maxCache = 5000;

    private final List<String> processingFileList = new ArrayList<>();
    private final Map<String, Painting> paintingMap = new ConcurrentHashMap<>();
    private final Map<String, File> fileMap = new ConcurrentHashMap<>();

    private CacheCenter() {
        if (!this.cachePath.exists()) {
            this.cachePath.mkdirs();
        }
        Logger.i(this.getClass(), "Cache path: " + this.cachePath.getAbsolutePath());
    }

    public final static CacheCenter getInstance() {
        return CacheCenter.instance;
    }

    public boolean isProcessing(String fileCode) {
        synchronized (this) {
            return this.processingFileList.contains(fileCode);
        }
    }

    public void markProcessing(String fileCode) {
        synchronized (this) {
            this.processingFileList.add(fileCode);
        }
    }

    public void finishProcessing(String fileCode) {
        synchronized (this) {
            this.processingFileList.remove(fileCode);
        }
    }

    public Painting getPainting(String fileCode) {
        return this.paintingMap.get(fileCode);
    }

    public void cache(String fileCode, Painting painting) {
        synchronized (this) {
            this.processingFileList.remove(fileCode);
        }
        this.paintingMap.put(fileCode, painting);
        tickCache();
    }

    public byte[] getData(String fileCode) {
        File file = this.fileMap.get(fileCode);
        if (null == file) {
            // 从本地磁盘加载
            file = new File(this.cachePath, fileCode);
        }
        if (!file.exists()) {
            this.fileMap.remove(fileCode);
            return null;
        }
        try {
            return Files.readAllBytes(Paths.get(file.getAbsolutePath()));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void cache(String fileCode, byte[] data) {
        try {
            File file = new File(this.cachePath, fileCode);
            Files.write(Paths.get(file.getAbsolutePath()), data);
            if (file.exists()) {
                this.fileMap.put(fileCode, file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected synchronized void tickCache() {
        if (!this.running.get()) {
            this.running.set(true);

            (new Thread(new Runnable() {
                @Override
                public void run() {
                    while (paintingMap.size() >= maxCache) {
                        long minTimestamp = System.currentTimeMillis();
                        String key = null;
                        Iterator<Map.Entry<String, Painting>> iter = paintingMap.entrySet().iterator();
                        while (iter.hasNext()) {
                            Map.Entry<String, Painting> entry = iter.next();
                            if (entry.getValue().timestamp < minTimestamp) {
                                minTimestamp = entry.getValue().timestamp;
                                key = entry.getKey();
                            }
                        }
                        if (null != key) {
                            paintingMap.remove(key);
                            File file = fileMap.remove(key);
                            if (null != file && file.exists()) {
                                file.delete();
                            }
                        }
                    }

                    running.set(false);
                }
            })).start();
        }
    }
}
