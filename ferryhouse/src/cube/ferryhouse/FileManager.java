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

package cube.ferryhouse;

import cell.core.talk.PrimitiveInputStream;
import cell.util.Cryptology;
import cell.util.log.Logger;
import cube.common.entity.FileLabel;
import cube.ferry.BoxReport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 文件管理器。
 */
public final class FileManager {

    private String domainName;

    private FerryStorage storage;

    private Path filePath;

    private final int maxThreadNum = 2;

    private AtomicInteger threadCount = new AtomicInteger(0);

    private Queue<PrimitiveInputStream> streamQueue = new ConcurrentLinkedQueue<>();

    public FileManager(String domainName, FerryStorage ferryStorage) {
        this.domainName = domainName;
        this.storage = ferryStorage;

        String path = Cryptology.getInstance().hashWithMD5AsString(domainName.getBytes(StandardCharsets.UTF_8));
        this.filePath = Paths.get(path + "/files");
        if (!Files.exists(this.filePath)) {
            try {
                Files.createDirectories(this.filePath);
            } catch (IOException e) {
            }
        }
    }

    /**
     * 保存文件标签。
     *
     * @param fileLabel
     */
    public void saveFileLabel(FileLabel fileLabel) {
        this.storage.writeFileLabel(fileLabel);
    }

    /**
     * 保存文件流。
     *
     * @param primitiveInputStream
     */
    public void saveFileInputStream(PrimitiveInputStream primitiveInputStream) {
        // 将流添加到队列
        this.streamQueue.offer(primitiveInputStream);

        if (this.threadCount.get() >= this.maxThreadNum) {
            return;
        }

        this.threadCount.incrementAndGet();

        (new Thread() {
            @Override
            public void run() {
                PrimitiveInputStream inputStream = streamQueue.poll();
                while (null != inputStream) {
                    String filename = inputStream.getName();
                    FileOutputStream fileOutputStream = null;
                    try {
                        fileOutputStream = new FileOutputStream(new File(filePath.toString(), filename));

                        byte[] bytes = new byte[256];
                        int length = 0;
                        while ((length = inputStream.read(bytes)) > 0) {
                            fileOutputStream.write(bytes, 0, length);
                        }
                    } catch (IOException e) {
                        Logger.w(FileManager.class, "#saveFileInputStream", e);
                    } finally {
                        if (null != fileOutputStream) {
                            try {
                                fileOutputStream.close();
                            } catch (IOException e) {
                                // Nothing
                            }
                        }

                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            // Nothing
                        }
                    }

                    inputStream = streamQueue.poll();
                }

                // 更新线程数量计数
                threadCount.decrementAndGet();
            }
        }).start();
    }

    public synchronized void calcUsage(BoxReport report) {
        List<FileLabel> fileLabels = this.storage.getAllFileLabels();

    }
}
