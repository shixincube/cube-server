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

package cube.service.fileprocessor;

import cell.util.log.Logger;
import cube.common.entity.FileLabel;
import cube.common.entity.FileThumbnail;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.plugin.PluginSystem;
import cube.service.filestorage.FileStorageService;
import cube.util.FileType;
import cube.util.FileUtils;
import net.coobird.thumbnailator.Thumbnails;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;

/**
 * 文件处理服务。
 */
public class FileProcessorService extends AbstractModule {

    public final static String NAME = "FileProcessor";

    private ExecutorService executor = null;

    private Path workPath;

    private CVConnector cvConnector;

    public FileProcessorService(ExecutorService executor) {
        super();
        this.executor = executor;
    }

    @Override
    public void start() {
        this.workPath = Paths.get("storage/tmp");
        if (!Files.exists(this.workPath)) {
            try {
                Files.createDirectories(this.workPath);
            } catch (IOException e) {
                Logger.e(this.getClass(), "#start", e);
            }
        }

        this.cvConnector = new CVConnector("DJLService",
                this.getKernel().getNucleus().getTalkService());
        this.cvConnector.start("127.0.0.1", 7711);
    }

    @Override
    public void stop() {
        this.cvConnector.stop();
    }

    @Override
    public PluginSystem<?> getPluginSystem() {
        return null;
    }

    @Override
    public void onTick(cube.core.Module module, Kernel kernel) {

    }

    /**
     * 生成缩略图。
     *
     * @param domainName
     * @param fileCode
     * @param size
     * @param quality
     * @return
     */
    public FileThumbnail makeThumbnail(String domainName, String fileCode, int size, double quality) {
        FileStorageService fileStorage = (FileStorageService) this.getKernel().getModule(FileStorageService.NAME);

        // 查找文件
        FileLabel srcFileLabel = fileStorage.getFile(domainName, fileCode);
        if (null == srcFileLabel) {
            return null;
        }

        return this.makeThumbnail(domainName, srcFileLabel, size, quality);
    }

    /**
     * 生成缩略图。
     *
     * @param domainName
     * @param srcFileLabel
     * @param size
     * @param quality
     * @return
     */
    public FileThumbnail makeThumbnail(String domainName, FileLabel srcFileLabel, int size, double quality) {
        boolean supported = false;
        FileType fileType = srcFileLabel.getFileType();
        if (fileType == FileType.JPEG || fileType == FileType.PNG || fileType == FileType.GIF) {
            supported = true;
        }

        if (!supported) {
            // 不支持的格式
            return null;
        }

        FileStorageService fileStorage = (FileStorageService) this.getKernel().getModule(FileStorageService.NAME);

        String fileCode = srcFileLabel.getFileCode();

        // 本地路径
        String path = fileStorage.loadFileToDisk(domainName, fileCode);
        if (null == path) {
            return null;
        }

        Logger.d(this.getClass(), "#makeThumbnail - file path: " + path);

        File input = new File(path);
        if (!input.exists()) {
            Logger.w(this.getClass(), "#makeThumbnail - can not find file: " + path);
            return null;
        }

        FileThumbnail fileThumbnail = null;

        // 生成缩略图文件名
        String thumbFileName = FileUtils.extractFileName(srcFileLabel.getFileName()) + "_thumb_" + size + ".jpg";

        // 生成缩略图文件码
        String thumbFileCode = FileUtils.makeFileCode(srcFileLabel.getOwnerId(), domainName, thumbFileName);

        // 输出文件
        String outputFile = Paths.get(this.workPath.toString(), thumbFileCode).toString();

        int srcWidth = 0;
        int srcHeight = 0;

        int thumbWidth = 0;
        int thumbHeight = 0;

        FileInputStream fis = null;

        try {
            fis = new FileInputStream(input);

            Thumbnails.Builder<? extends InputStream> fileBuilder = Thumbnails.of(fis).scale(1.0).outputQuality(1.0);
//            Thumbnails.Builder<File> fileBuilder = Thumbnails.of(input).scale(1.0).outputQuality(1.0);

            BufferedImage src = fileBuilder.asBufferedImage();
            srcWidth = src.getWidth();
            srcHeight = src.getHeight();

            if (srcWidth > size || srcHeight > size) {
                Thumbnails.Builder<File> builder = Thumbnails.of(input);
                builder.size(size, size).outputFormat("jpg").outputQuality(quality).toFile(outputFile);

                BufferedImage thumb = builder.asBufferedImage();
                thumbWidth = thumb.getWidth();
                thumbHeight = thumb.getHeight();
                thumb = null;
            }
            else {
                Thumbnails.of(input).scale(1.0).outputQuality(quality).toFile(outputFile);

                thumbWidth = srcWidth;
                thumbHeight = srcHeight;
            }

            src = null;
        } catch (IOException e) {
            Logger.e(this.getClass(), "#makeThumbnail", e);
            return null;
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // Nothing
                }
            }
        }

        // 写入到文件系统
        File thumbFile = new File(outputFile + ".jpg");
        fileStorage.writeFile(thumbFileCode, thumbFile);

        // 删除临时文件
        thumbFile.delete();

        // 放置文件标签
        FileLabel fileLabel = new FileLabel(domainName, thumbFileCode, srcFileLabel.getOwnerId(),
                thumbFileName, thumbFile.length(), System.currentTimeMillis(), srcFileLabel.getExpiryTime());
        fileLabel.setFileType(FileType.JPEG);
        fileLabel = fileStorage.putFile(fileLabel);

        // 创建文件缩略图
        fileThumbnail = new FileThumbnail(fileLabel, thumbWidth, thumbHeight,
                srcFileLabel.getFileCode(), srcWidth, srcHeight, quality);

        return fileThumbnail;
    }

    public CVResult detectObject(String domainName, String fileCode) {
        FileStorageService storageService = (FileStorageService) this.getKernel().getModule(FileStorageService.NAME);
        String path = storageService.loadFileToDisk(domainName, fileCode);

        File file = new File(path);
        if (!file.exists()) {
            Logger.w(this.getClass(), "#detectImageObject - can not find file: " + path);
            return null;
        }

        final Object mutex = new Object();
        final CVResult cvResult = new CVResult();

        this.cvConnector.detectObjects(file, fileCode, new CVCallback() {
            @Override
            public void handleSuccess(CVResult result) {
                cvResult.set(result);

                synchronized (mutex) {
                    mutex.notify();
                }
            }

            @Override
            public void handleFailure(CVResult result) {
                cvResult.set(result);

                synchronized (mutex) {
                    mutex.notify();
                }
            }
        });

        synchronized (mutex) {
            try {
                mutex.wait(40L * 1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return cvResult;
    }
}
