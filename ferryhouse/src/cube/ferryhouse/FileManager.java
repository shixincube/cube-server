/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.ferryhouse;

import cell.core.talk.PrimitiveInputStream;
import cell.util.Cryptology;
import cell.util.log.Logger;
import cube.common.entity.FileLabel;
import cube.ferry.BoxReport;
import cube.ferryhouse.command.DiskUsage;
import cube.util.FileType;
import cube.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private long maxSpaceSize = 1024L * 1024 * 1024;

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

    public void setMaxSpaceSize(long value) {
        this.maxSpaceSize = value;
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

    /**
     * 清空文件数据。
     */
    public void cleanup() {
        File[] files = this.filePath.toFile().listFiles();
        if (null != files && files.length > 0) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    public synchronized void calcUsage(BoxReport report) {
        long imageTotalSize = 0;
        long docTotalSize = 0;
        long videoTotalSize = 0;
        long audioTotalSize = 0;
        long packageTotalSize = 0;
        long otherTotalSize = 0;

        int imageNum = 0;
        int docNum = 0;
        int videoNum = 0;
        int audioNum = 0;
        int packageNum = 0;
        int otherNum = 0;

        File[] files = this.filePath.toFile().listFiles();
        if (null != files && files.length > 0) {
            // 计算文件占用空间
            for (File file : files) {
                String fileCode = file.getName();
                FileLabel fileLabel = this.storage.readFileLabel(fileCode);
                if (null == fileLabel) {
                    Logger.w(this.getClass(), "Can NOT find file in DB - " + fileCode);
                    continue;
                }

                FileType fileType = fileLabel.getFileType();
                if (FileUtils.isImageType(fileType)) {
                    imageTotalSize += fileLabel.getFileSize();
                    imageNum += 1;
                }
                else if (FileUtils.isDocumentType(fileType)) {
                    docTotalSize += fileLabel.getFileSize();
                    docNum += 1;
                }
                else if (FileUtils.isVideoType(fileType)) {
                    videoTotalSize += fileLabel.getFileSize();
                    videoNum += 1;
                }
                else if (FileUtils.isAudioType(fileType)) {
                    audioTotalSize += fileLabel.getFileSize();
                    audioNum += 1;
                }
                else {
                    if (fileType == FileType.ZIP || fileType == FileType.RAR ||
                        fileType == FileType._7Z || fileType == FileType.Z || fileType == FileType.TAR) {
                        packageTotalSize += fileLabel.getFileSize();
                        packageNum += 1;
                    }
                    else {
                        otherTotalSize += fileLabel.getFileSize();
                        otherNum += 1;
                    }
                }
            }
        }

        report.setImageFilesUsedSize(imageTotalSize);
        report.setDocFilesUsedSize(docTotalSize);
        report.setVideoFilesUsedSize(videoTotalSize);
        report.setAudioFilesUsedSize(audioTotalSize);
        report.setPackageFilesUsedSize(packageTotalSize);
        report.setOtherFilesUsedSize(otherTotalSize);

        report.setNumImageFiles(imageNum);
        report.setNumDocFiles(docNum);
        report.setNumVideoFiles(videoNum);
        report.setNumAudioFiles(audioNum);
        report.setNumPackageFiles(packageNum);
        report.setNumOtherFiles(otherNum);

        DiskUsage command = new DiskUsage();
        try {
            command.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 计算可用空间
        long freeSize = this.maxSpaceSize - imageTotalSize - docTotalSize - videoTotalSize - audioTotalSize
                - packageTotalSize - otherTotalSize;

        report.setFreeDiskSize(Math.min(command.getAvailInBytes(), freeSize));
    }
}
