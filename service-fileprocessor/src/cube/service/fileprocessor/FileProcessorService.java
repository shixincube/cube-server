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

package cube.service.fileprocessor;

import cell.util.log.Logger;
import cube.common.action.FileProcessorAction;
import cube.common.entity.FileLabel;
import cube.common.entity.FileThumbnail;
import cube.common.entity.Image;
import cube.common.state.FileProcessorStateCode;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.file.FileProcessResult;
import cube.file.OperationWork;
import cube.file.OperationWorkflow;
import cube.plugin.PluginSystem;
import cube.service.fileprocessor.processor.*;
import cube.service.fileprocessor.processor.video.VideoProcessor;
import cube.service.fileprocessor.processor.video.VideoProcessorBuilder;
import cube.service.fileprocessor.processor.video.VideoProcessorContext;
import cube.service.filestorage.FileStorageService;
import cube.util.ConfigUtils;
import cube.util.FileType;
import cube.util.FileUtils;
import net.coobird.thumbnailator.Thumbnails;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 文件处理服务。
 */
public class FileProcessorService extends AbstractModule {

    public final static String NAME = "FileProcessor";

    private ExecutorService executor = null;

    private Path workPath;

    private CVConnector cvConnector;

    private boolean useImageMagick = true;

    /**
     * 插件系统。
     */
    protected ProcessorPluginSystem pluginSystem;

    public FileProcessorService(ExecutorService executor) {
        super();
        this.executor = executor;
    }

    @Override
    public void start() {
        this.pluginSystem = new ProcessorPluginSystem();

        // 工具校验
        this.useImageMagick = ImageTools.check();

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
//        this.cvConnector.start("127.0.0.1", 7711);

        // 加载配置
        this.loadConfig();
    }

    @Override
    public void stop() {
        this.cvConnector.stop();
    }

    @Override
    public PluginSystem<?> getPluginSystem() {
        return this.pluginSystem;
    }

    @Override
    public void onTick(cube.core.Module module, Kernel kernel) {

    }

    private void loadConfig() {
        Path path = Paths.get("config/file-storage.properties");
        if (!Files.exists(path)) {
            path = Paths.get("file-storage.properties");
            if (!Files.exists(path)) {
                return;
            }
        }

        Properties properties = null;
        try {
            properties = ConfigUtils.readProperties(path.toString());
        } catch (IOException e) {
            // Nothing
        }

        if (null == properties) {
            return;
        }

        String thumbnail = properties.getProperty("thumbnail", "ImageMagick");
        if (thumbnail.equalsIgnoreCase("ImageMagick")) {
            this.enableImageMagick();
            Logger.i(this.getClass(), "Enable ImageMagick");
        }
        else {
            this.disableImageMagick();
            Logger.i(this.getClass(), "Disable ImageMagick");
        }
    }

    /**
     * 启用 ImageMagick 进行图片处理。
     */
    public void enableImageMagick() {
        this.useImageMagick = true;
    }

    /**
     * 禁用 ImageMagick 进行图片处理。
     */
    public void disableImageMagick() {
        this.useImageMagick = false;
    }

    /**
     * 生成缩略图。
     *
     * @param domainName
     * @param fileCode
     * @param quality
     * @return
     */
    public FileThumbnail makeThumbnail(String domainName, String fileCode, int quality) {
        FileStorageService fileStorage = (FileStorageService) this.getKernel().getModule(FileStorageService.NAME);

        // 查找文件
        FileLabel srcFileLabel = fileStorage.getFile(domainName, fileCode);
        if (null == srcFileLabel) {
            return null;
        }

        return this.makeThumbnail(domainName, srcFileLabel, quality);
    }

    /**
     * 生成缩略图。
     *
     * @param domainName
     * @param srcFileLabel
     * @param quality
     * @return
     */
    public FileThumbnail makeThumbnail(String domainName, FileLabel srcFileLabel, int quality) {
        boolean supported = false;
        FileType fileType = srcFileLabel.getFileType();
        if (fileType == FileType.JPEG || fileType == FileType.PNG
                || fileType == FileType.GIF || fileType == FileType.BMP) {
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
        String thumbFileName = FileUtils.extractFileName(srcFileLabel.getFileName()) + "_thumb.jpg";

        // 生成缩略图文件码
        String thumbFileCode = FileUtils.makeFileCode(srcFileLabel.getOwnerId(), domainName, thumbFileName);

        // 输出文件
        String outputFile = Paths.get(this.workPath.toString(), thumbFileCode).toString();

        int srcWidth = 0;
        int srcHeight = 0;

        int thumbWidth = 0;
        int thumbHeight = 0;

        if (this.useImageMagick) {
            Image image = ImageTools.identify(input.getAbsolutePath());
            if (null == image) {
                Logger.w(this.getClass(), "#makeThumbnail - Can NOT identify input file : " + input.getAbsolutePath());
                return null;
            }

            srcWidth = image.width;
            srcHeight = image.height;

            // 生成缩略图
            Image thumbImage = ImageTools.thumbnail(input.getAbsolutePath(), outputFile, quality);

            if (null == thumbImage) {
                Logger.w(this.getClass(), "#makeThumbnail - Can NOT make thumbnail image : " + input.getAbsolutePath());
                return null;
            }

            thumbWidth = thumbImage.width;
            thumbHeight = thumbImage.height;
        }
        else {
            FileInputStream fis = null;

            try {
                fis = new FileInputStream(input);

                Thumbnails.Builder<? extends InputStream> fileBuilder = Thumbnails.of(fis).scale(1.0).outputQuality(1.0);
//                Thumbnails.Builder<File> fileBuilder = Thumbnails.of(input).scale(1.0).outputQuality(1.0);

                BufferedImage src = fileBuilder.asBufferedImage();
                srcWidth = src.getWidth();
                srcHeight = src.getHeight();

                int size = Math.max(srcWidth, srcHeight);

                if (srcWidth > size || srcHeight > size) {
                    Thumbnails.Builder<File> builder = Thumbnails.of(input);
                    builder.size(size, size).outputFormat("jpg").outputQuality(((double)quality) / 100.0f).toFile(outputFile);

                    BufferedImage thumb = builder.asBufferedImage();
                    thumbWidth = thumb.getWidth();
                    thumbHeight = thumb.getHeight();
                    thumb = null;
                }
                else {
                    Thumbnails.of(input).scale(1.0).outputQuality(((double)quality) / 100.0f).toFile(outputFile);

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
        }

        // 写入到文件系统
        File thumbFile = new File(outputFile + ".jpg");
        if (thumbFile.exists()) {
            // 写入到文件存储
            fileStorage.writeFile(thumbFileCode, thumbFile);

            // 放置文件标签
            FileLabel fileLabel = new FileLabel(domainName, thumbFileCode, srcFileLabel.getOwnerId(),
                    thumbFileName, thumbFile.length(), thumbFile.lastModified(),
                    System.currentTimeMillis(), srcFileLabel.getExpiryTime());
            fileLabel.setFileType(FileType.JPEG);
            fileLabel = fileStorage.putFile(fileLabel);

            // 创建文件缩略图
            fileThumbnail = new FileThumbnail(fileLabel, thumbWidth, thumbHeight,
                    srcFileLabel.getFileCode(), srcWidth, srcHeight, quality);

            // 删除临时文件
            thumbFile.delete();

            return fileThumbnail;
        }
        else {
            return null;
        }
    }

    public CVResult detectObject(String domainName, String fileCode) {
        FileStorageService storageService = (FileStorageService) this.getKernel().getModule(FileStorageService.NAME);
        FileLabel label = storageService.getFile(domainName, fileCode);
        if (null == label) {
            Logger.w(this.getClass(), "#detectObject - can not find file label: " + fileCode);
            return null;
        }

        String path = storageService.loadFileToDisk(domainName, fileCode);

        File file = new File(path);
        if (!file.exists()) {
            Logger.w(this.getClass(), "#detectObject - can not find file: " + path);
            return null;
        }

        final Object mutex = new Object();
        final CVResult cvResult = new CVResult();
        final AtomicBoolean failure = new AtomicBoolean(false);

        this.cvConnector.detectObjects(file, fileCode, new CVCallback() {
            @Override
            public void handleSuccess(CVResult result) {
                cvResult.set(result);

                synchronized (mutex) {
                    mutex.notify();
                }
            }

            @Override
            public void handleFailure(FileProcessorStateCode stateCode, CVResult result) {
                Logger.w(FileStorageService.class, "#detectObject - failure : " + stateCode.code);
                failure.set(true);
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

        if (failure.get()) {
            return null;
        }

        return cvResult;
    }

    /**
     * 创建图像处理器。
     *
     * @param domainName
     * @param fileCode
     * @return
     */
    public ImageProcessor createImageProcessor(String domainName, String fileCode) {
        FileStorageService storageService = (FileStorageService) this.getKernel().getModule(FileStorageService.NAME);
        FileLabel fileLabel = storageService.getFile(domainName, fileCode);
        if (null == fileLabel) {
            return null;
        }

        if (!FileUtils.isImageType(fileLabel.getFileType())) {
            // 指定的文件不是图片格式
            Logger.w(this.getClass(), "File is NOT image : " + fileLabel.getFileName());
            return null;
        }

        Path imageFile = Paths.get(this.workPath.toString(), fileCode + "." + fileLabel.getFileType().getPreferredExtension());

        if (!this.existsFile(fileCode, fileLabel.getFileType().getPreferredExtension())) {
            String path = storageService.loadFileToDisk(domainName, fileCode);
            if (null == path) {
                return null;
            }

            try {
                Files.copy(Paths.get(path), imageFile);
            } catch (IOException e) {
                Logger.e(this.getClass(), "#createImageProcessor", e);
                return null;
            }
        }

        ImageProcessor imageProcessor = new ImageProcessor(this.workPath);
        imageProcessor.setImageFile(imageFile.toFile(), fileLabel);
        return imageProcessor;
    }

    /**
     * 创建指定的 OCR 处理器。
     *
     * @param domainName
     * @param fileCode
     * @return
     */
    public OCRProcessor createOCRProcessor(String domainName, String fileCode) {
        FileStorageService storageService = (FileStorageService) this.getKernel().getModule(FileStorageService.NAME);
        FileLabel fileLabel = storageService.getFile(domainName, fileCode);
        if (null == fileLabel) {
            return null;
        }

        if (!FileUtils.isImageType(fileLabel.getFileType())) {
            // 指定的文件不是图片格式
            Logger.w(this.getClass(), "File is NOT image : " + fileLabel.getFileName());
            return null;
        }

        Path imageFile = Paths.get(this.workPath.toString(), fileCode + "." + fileLabel.getFileType().getPreferredExtension());

        if (!this.existsFile(fileCode, fileLabel.getFileType().getPreferredExtension())) {
            String path = storageService.loadFileToDisk(domainName, fileCode);
            if (null == path) {
                return null;
            }

            try {
                Files.copy(Paths.get(path), imageFile);
            } catch (IOException e) {
                Logger.e(this.getClass(), "#createOCRProcessor", e);
                return null;
            }
        }

        OCRProcessor ocrProcessor = new OCRProcessor(this.workPath);
        ocrProcessor.setImageFile(fileLabel);
        ocrProcessor.setInputImage(imageFile.toFile());

        return ocrProcessor;
    }

    /**
     * 创建视频处理器。
     *
     * @param domainName
     * @param fileCode
     * @param operationJson
     * @return
     */
    public VideoProcessor createVideoProcessor(String domainName, String fileCode, JSONObject operationJson) {
        FileStorageService storageService = (FileStorageService) this.getKernel().getModule(FileStorageService.NAME);
        FileLabel fileLabel = storageService.getFile(domainName, fileCode);
        if (null == fileLabel) {
            return null;
        }

        if (!FileUtils.isVideoType(fileLabel.getFileType())) {
            // 指定的文件不是视频文件
            Logger.w(this.getClass(), "File is NOT video : " + fileLabel.getFileName());
            return null;
        }

        Path videoFile = Paths.get(this.workPath.toString(), fileCode + "." + fileLabel.getFileType().getPreferredExtension());

        if (!this.existsFile(fileCode, fileLabel.getFileType().getPreferredExtension())) {
            String path = storageService.loadFileToDisk(domainName, fileCode);
            if (null == path) {
                return null;
            }

            try {
                Files.copy(Paths.get(path), videoFile);
            } catch (IOException e) {
                Logger.e(this.getClass(), "#createSnapshotFrameProcessor", e);
                return null;
            }
        }

        VideoProcessor processor = VideoProcessorBuilder.build(this.workPath, operationJson);
        processor.setInputFile(videoFile.toFile(), fileLabel);
        return processor;
    }

    private boolean existsFile(String fileCode, String fileType) {
        Path path = Paths.get(this.workPath.toString(), fileCode + "." + fileType);
        return Files.exists(path);
    }

    /**
     * 异步加载工作流。
     *
     * @param executor
     * @param workflow
     * @return 返回是否成功启动工作流。
     */
    public boolean launchOperationWorkFlow(ExecutorService executor, OperationWorkflow workflow) {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                launchOperationWorkFlow(workflow);
            }
        });
        return true;
    }

    /**
     * 加载操作工作流。
     *
     * @param workflow
     * @return 返回是否成功启动工作流。
     */
    public boolean launchOperationWorkFlow(OperationWorkflow workflow) {
        // 域
        String domainName = workflow.getDomain();
        // 源文件的文件码
        String fileCode = workflow.getSourceFileCode();

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#launchOperationWorkFlow - Start workflow : " + workflow.getSN() +
                    " - @" + domainName + " - " + fileCode);
        }

        FileStorageService storageService = (FileStorageService) this.getKernel().getModule(FileStorageService.NAME);
        FileLabel fileLabel = storageService.getFile(domainName, fileCode);
        if (null == fileLabel) {
            Logger.e(this.getClass(), "#launchOperationWorkFlow - (" + workflow.getSN()
                    + ") Not find file : " + fileCode);
            return false;
        }

        Path sourceFile = Paths.get(this.workPath.toString(), fileCode + "." + fileLabel.getFileType().getPreferredExtension());

        if (!this.existsFile(fileCode, fileLabel.getFileType().getPreferredExtension())) {
            String path = storageService.loadFileToDisk(domainName, fileCode);
            if (null == path) {
                Logger.e(this.getClass(), "#launchOperationWorkFlow - (" + workflow.getSN()
                        + ") Load file to disk failed : " + fileCode);
                return false;
            }

            try {
                Files.copy(Paths.get(path), sourceFile);
            } catch (IOException e) {
                Logger.e(this.getClass(), "#launchOperationWorkFlow - (" + workflow.getSN() + ")", e);
                return false;
            }
        }

        // 调用钩子
        WorkflowPluginContext pluginContext = new WorkflowPluginContext(workflow);
        this.pluginSystem.getStartWorkflowHook().apply(pluginContext);

        List<OperationWork> workList = workflow.getWorkList();

        // 设置入口文件
        List<File> input = new ArrayList<>();
        input.add(sourceFile.toFile());
        workList.get(0).setInput(input);

        boolean interrupt = false;
        OperationWork prevWork = null;

        for (int i = 0, length = workList.size(); i < length; ++i) {
            OperationWork work = workList.get(i);

            prevWork = (i > 0) ? workList.get(i - 1) : null;

            List<File> inputFile = (null != prevWork) ? prevWork.getOutput() : work.getInput();
            if (null != prevWork) {
                work.setInput(inputFile);
            }

            pluginContext = new WorkflowPluginContext(workflow, work);
            this.pluginSystem.getStartWorkInWorkflowHook().apply(pluginContext);

            String process = work.getFileOperation().getProcessAction();

            if (FileProcessorAction.Image.name.equals(process)) {
                // 创建处理器
                ImageProcessor imageProcessor = new ImageProcessor(this.workPath);
                imageProcessor.setImageFile(inputFile.get(0));
                // 创建上下文
                ImageProcessorContext context = new ImageProcessorContext();
                context.parseOperation(work.getFileOperation());
                // 进行处理
                imageProcessor.go(context);

                if (context.isSuccessful()) {
                    // 设置当前工序的输出
                    List<File> output = new ArrayList<>();
                    output.add(context.getResultStream().file);
                    work.setOutput(output);
                    work.setProcessResult(new FileProcessResult(context.toJSON()));
                }
                else {
                    interrupt = true;
                    break;
                }
            }
            else if (FileProcessorAction.OCR.name.equals(process)) {

            }

            pluginContext = new WorkflowPluginContext(workflow, work);
            this.pluginSystem.getStopWorkInWorkflowHook().apply(pluginContext);
        }

        Logger.i(this.getClass(), "#launchOperationWorkFlow - (" + workflow.getSN() + ") interrupt : " + interrupt);

        if (!interrupt) {
            OperationWork lastWork = workList.get(workList.size() - 1);
            FileProcessResult result = lastWork.getProcessResult();
            if (null == result) {
                Logger.e(this.getClass(), "#launchOperationWorkFlow - (" + workflow.getSN()
                        + ") Result data error : " + fileCode);
                return false;
            }
        }

        // 删除过程文件
        for (int i = 0, len = workList.size() - 1; i < len; ++i) {
            OperationWork work = workList.get(i);
            List<File> output = work.getOutput();
            for (File file : output) {
                if (file.exists()) {
                    file.delete();
                }
            }
        }

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#launchOperationWorkFlow - End workflow : " + workflow.getSN());
        }

        // 调用钩子
        pluginContext = new WorkflowPluginContext(workflow);
        this.pluginSystem.getStopWorkflowHook().apply(pluginContext);

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object notify(Object event) {
        if (event instanceof JSONObject) {
            JSONObject data = (JSONObject) event;
            String action = data.getString("action");

            if (FileProcessorAction.Image.name.equals(action)) {
                String domain = data.getString("domain");
                String fileCode = data.getString("fileCode");
                // 创建图像处理器
                ImageProcessor processor = createImageProcessor(domain, fileCode);
                if (null != processor) {
                    ImageProcessorContext context = new ImageProcessorContext();
                    // 解析参数
                    context.parseOperation(data.getJSONObject("parameter"));
                    processor.go(context);
                    return context.toJSON();
                }
            }
            else if (FileProcessorAction.OCR.name.equals(action)) {
                String domain = data.getString("domain");
                String fileCode = data.getString("fileCode");
                // 创建 OCR 处理器
                OCRProcessor processor = createOCRProcessor(domain, fileCode);
                if (null != processor) {
                    OCRProcessorContext context = new OCRProcessorContext();
                    processor.go(context);
                    return context.toJSON();
                }
            }
            else if (FileProcessorAction.SubmitWorkflow.name.equals(action)) {
                JSONObject workflowJson = data.getJSONObject("workflow");
                // 解析工作流
                OperationWorkflow workflow = new OperationWorkflow(workflowJson);
                workflow.setClientId(data.getLong("clientId"));
                // 加载工作流
                boolean result = this.launchOperationWorkFlow(this.executor, workflow);
                WorkflowContext context = new WorkflowContext(action);
                context.setSuccessful(result);
                return context.toJSON();
            }
            else if (FileProcessorAction.CancelWorkflow.name.equals(action)) {
                // TODO
            }
            else if (FileProcessorAction.Video.name.equals(action)) {
                String domain = data.getString("domain");
                String fileCode = data.getString("fileCode");
                // 创建视频处理器
                VideoProcessor processor = createVideoProcessor(domain, fileCode, data.getJSONObject("parameter"));
                if (null != processor) {
                    VideoProcessorContext context = new VideoProcessorContext();
                    processor.go(context);
                    return context.toJSON();
                }
            }
        }

        return null;
    }
}
