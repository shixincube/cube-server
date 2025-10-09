/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.cv;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.common.Packet;
import cube.common.action.CVAction;
import cube.common.entity.*;
import cube.common.notice.GetFile;
import cube.common.notice.LoadFile;
import cube.common.state.CVStateCode;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.plugin.PluginSystem;
import cube.service.auth.AuthService;
import cube.service.cv.listener.ClipPaperListener;
import cube.service.cv.listener.CombineBarCodeListener;
import cube.service.cv.listener.DetectBarCodeListener;
import cube.service.cv.listener.DetectObjectListener;
import cube.util.FileType;
import cube.vision.Size;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 计算机视觉功能服务模块。
 */
public class CVService extends AbstractModule {

    public final static String NAME = "CV";

    private CVCellet cellet;

    private ExecutorService executor;

    private List<CVEndpoint> endpointList;

    public CVService(CVCellet cellet) {
        this.cellet = cellet;
        this.endpointList = new ArrayList<>();
    }

    @Override
    public void start() {
        this.executor = Executors.newFixedThreadPool(8);

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                // 设置工具
                AbstractModule fileStorage = getKernel().getModule("FileStorage");
                if (null != fileStorage) {
                    while (!fileStorage.isStarted()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                ToolKit.getInstance().setService(fileStorage);
            }
        });
    }

    @Override
    public void stop() {
        this.endpointList.clear();
        if (null != this.executor) {
            this.executor.shutdown();
        }
    }

    @Override
    public <T extends PluginSystem> T getPluginSystem() {
        return null;
    }

    @Override
    public void onTick(Module module, Kernel kernel) {
    }

    public void setup(Contact contact, TalkContext talkContext) {
        synchronized (this.endpointList) {
            for (CVEndpoint endpoint : this.endpointList) {
                if (endpoint.contact.getId().longValue() == contact.getId().longValue()) {
                    this.endpointList.remove(endpoint);
                    break;
                }
            }

            this.endpointList.add(new CVEndpoint(contact, talkContext));

            Logger.i(this.getClass(), "#setup - Setup id: " + contact.getId());
        }
    }

    public void teardown(Contact contact, TalkContext talkContext) {
        synchronized (this.endpointList) {
            for (CVEndpoint endpoint : this.endpointList) {
                if (endpoint.contact.getId().longValue() == contact.getId().longValue()) {
                    this.endpointList.remove(endpoint);
                    break;
                }
            }

            Logger.i(this.getClass(), "#teardown - Teardown id: " + contact.getId());
        }
    }

    /**
     * 获取令牌。
     *
     * @param tokenCode
     * @return
     */
    public AuthToken getToken(String tokenCode) {
        AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
        AuthToken authToken = authService.getToken(tokenCode);
        return authToken;
    }

    public FileLabel getFile(String domain, String fileCode) {
        AbstractModule fileStorage = this.getKernel().getModule("FileStorage");
        if (null == fileStorage) {
            Logger.e(this.getClass(), "#getFile - File storage service is not ready");
            return null;
        }

        GetFile getFile = new GetFile(domain, fileCode);
        JSONObject fileLabelJson = fileStorage.notify(getFile);
        if (null == fileLabelJson) {
            Logger.e(this.getClass(), "#getFile - Get file failed: " + fileCode);
            return null;
        }

        return new FileLabel(fileLabelJson);
    }

    public File loadFile(String domain, String fileCode) {
        AbstractModule fileStorage = this.getKernel().getModule("FileStorage");
        if (null == fileStorage) {
            Logger.e(this.getClass(), "#loadFile - File storage service is not ready");
            return null;
        }

        LoadFile loadFile = new LoadFile(domain, fileCode);
        try {
            String path = fileStorage.notify(loadFile);
            return new File(path);
        } catch (Exception e) {
            Logger.e(this.getClass(), "#loadFile - File storage service load failed", e);
            return null;
        }
    }

    /**
     * 合并二维码到文件。
     *
     * @param token
     * @param barcodeList
     * @return
     */
    public boolean combineBarcodes(AuthToken token, List<BarCode> barcodeList, CombineBarCodeListener listener) {
        final CVEndpoint endpoint = this.selectEndpoint();
        if (null == endpoint) {
            Logger.e(this.getClass(), "#combineBarcodes - No endpoints");
            return false;
        }

        endpoint.setWorking(true);

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject payload = new JSONObject();
                    JSONArray barcodes = new JSONArray();
                    for (BarCode barCode : barcodeList) {
                        barcodes.put(barCode.toJSON());
                    }
                    payload.put("barcodes", barcodes);
                    Packet request = new Packet(CVAction.CombineBarcodes.name, payload);
                    ActionDialect dialect = cellet.transmit(endpoint.talkContext, request.toDialect(), 3 * 60 * 1000);
                    if (null == dialect) {
                        Logger.w(this.getClass(), "#combineBarcodes - Endpoint is error");
                        listener.onFailed(barcodeList, CVStateCode.EndpointException);
                        return;
                    }

                    Packet response = new Packet(dialect);
                    if (Packet.extractCode(response) != CVStateCode.Ok.code) {
                        Logger.w(this.getClass(), "#combineBarcodes - Process failed : " + Packet.extractCode(response));
                        listener.onFailed(barcodeList, CVStateCode.FileError);
                        return;
                    }

                    JSONObject data = Packet.extractDataPayload(response);
                    JSONObject fileLabelJson = data.getJSONObject("fileLabel");
                    FileLabel fileLabel = new FileLabel(fileLabelJson);

                    // 从文件系统检查文件
                    int countdown = 10;
                    FileLabel current = null;
                    do {
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            // Nothing
                        }
                        current = getFile(token.getDomain(), fileLabel.getFileCode());
                        --countdown;
                        if (countdown <= 0) {
                            break;
                        }
                    } while (null == current);

                    if (null != current) {
                        listener.onCompleted(barcodeList, fileLabel);
                    }
                    else {
                        Logger.e(this.getClass(), "#combineBarcodes - Endpoint upload file failed: " + endpoint.toString());
                        listener.onFailed(barcodeList, CVStateCode.FileError);
                    }
                } catch (Exception e) {
                    Logger.e(this.getClass(), "#combineBarcodes - Error", e);
                    listener.onFailed(barcodeList, CVStateCode.Failure);
                } finally {
                    endpoint.setWorking(false);
                }
            }
        });

        return true;
    }

    /**
     * 检测并解码条形码。
     *
     * @param token
     * @param fileCodes
     * @param listener
     * @return
     */
    public boolean detectBarCode(AuthToken token, List<String> fileCodes, DetectBarCodeListener listener) {
        final CVEndpoint endpoint = this.selectEndpoint();
        if (null == endpoint) {
            Logger.e(this.getClass(), "#detectBarCode - No endpoints");
            return false;
        }

        endpoint.setWorking(true);

        final int limit = 10;
        final List<FileLabel> fileLabels = new ArrayList<>();
        for (String fileCode : fileCodes) {
            FileLabel fileLabel = this.getFile(token.getDomain(), fileCode);
            if (null != fileLabel) {
                if (fileLabel.getFileType() == FileType.JPEG ||
                        fileLabel.getFileType() == FileType.PNG ||
                        fileLabel.getFileType() == FileType.BMP) {
                    fileLabels.add(fileLabel);
                    if (fileLabels.size() >= limit) {
                        // 超限
                        break;
                    }
                }
            }
        }

        if (fileLabels.isEmpty()) {
            endpoint.setWorking(false);
            Logger.e(this.getClass(), "#detectBarCode - Can NOT find files");
            return false;
        }

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    List<BarCodeInfo> infoList = new ArrayList<>();

                    for (FileLabel fileLabel : fileLabels) {
                        JSONObject payload = new JSONObject();
                        payload.put("file", fileLabel.toJSON());
                        Packet request = new Packet(CVAction.DetectBarCode.name, payload);
                        ActionDialect dialect = cellet.transmit(endpoint.talkContext, request.toDialect(), 60 * 1000);
                        if (null == dialect) {
                            Logger.w(this.getClass(), "#detectBarCode - Endpoint is error");
                            continue;
                        }

                        Packet response = new Packet(dialect);
                        if (Packet.extractCode(response) != CVStateCode.Ok.code) {
                            Logger.d(this.getClass(), "#detectBarCode - Process failed");
                            continue;
                        }

                        List<BarCode> codeList = new ArrayList<>();

                        JSONObject data = Packet.extractDataPayload(response);
                        JSONArray result = data.getJSONArray("result");
                        for (int i = 0; i < result.length(); ++i) {
                            BarCode barCode = new BarCode(result.getJSONObject(i));
                            codeList.add(barCode);

                            Logger.d(this.getClass(), "#detectBarCode - result: " + barCode.data +
                                    " from " + fileLabel.getFileCode());
                        }

                        BarCodeInfo info = new BarCodeInfo(fileLabel, codeList);
                        infoList.add(info);
                    }

                    listener.onCompleted(infoList);
                } finally {
                    endpoint.setWorking(false);
                }
            }
        });

        return true;
    }

    /**
     * 检测图片的物体。
     *
     * @param token
     * @param fileCodes
     * @param listener
     * @return
     */
    public boolean detectObject(AuthToken token, List<String> fileCodes, DetectObjectListener listener) {
        final CVEndpoint endpoint = this.selectEndpoint();
        if (null == endpoint) {
            Logger.e(this.getClass(), "#detectObject - No endpoints");
            return false;
        }

        endpoint.setWorking(true);

        final int limit = 10;
        final List<FileLabel> fileLabels = new ArrayList<>();
        for (String fileCode : fileCodes) {
            FileLabel fileLabel = this.getFile(token.getDomain(), fileCode);
            if (null != fileLabel) {
                if (fileLabel.getFileType() == FileType.JPEG ||
                        fileLabel.getFileType() == FileType.PNG ||
                        fileLabel.getFileType() == FileType.BMP) {
                    fileLabels.add(fileLabel);
                    if (fileLabels.size() >= limit) {
                        // 超限
                        break;
                    }
                }
            }
        }

        if (fileLabels.isEmpty()) {
            endpoint.setWorking(false);
            Logger.e(this.getClass(), "#detectObject - Can NOT find files");
            return false;
        }

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONArray list = new JSONArray();
                    for (FileLabel fileLabel : fileLabels) {
                        list.put(fileLabel.toJSON());
                    }

                    JSONObject payload = new JSONObject();
                    payload.put("list", list);
                    Packet request = new Packet(CVAction.ObjectDetection.name, payload);
                    ActionDialect dialect = cellet.transmit(endpoint.talkContext, request.toDialect(), 3 * 60 * 1000);
                    if (null == dialect) {
                        Logger.w(this.getClass(), "#detectObject - Endpoint is error");
                        listener.onFailed(fileCodes, CVStateCode.EndpointException);
                        return;
                    }

                    Packet response = new Packet(dialect);
                    if (Packet.extractCode(response) != CVStateCode.Ok.code) {
                        Logger.d(this.getClass(), "#detectObject - Process failed");
                        listener.onFailed(fileCodes, CVStateCode.Failure);
                        return;
                    }

                    List<ObjectInfo> objectList = new ArrayList<>();

                    JSONObject data = Packet.extractDataPayload(response);
                    JSONArray result = data.getJSONArray("result");
                    for (int i = 0; i < result.length(); ++i) {
                        ObjectInfo info = new ObjectInfo(result.getJSONObject(i));
                        info.setFileLabel(findFileLabel(fileLabels, info.getFileCode()));
                        objectList.add(info);
                    }

                    long elapsed = data.getLong("elapsed");
                    listener.onCompleted(objectList, elapsed);
                } finally {
                    endpoint.setWorking(false);
                }
            }
        });

        return true;
    }

    public ObjectInfo detectObject(AuthToken token, String fileCode) {
        List<String> list = new ArrayList<>();
        list.add(fileCode);

        final List<ObjectInfo> result = new ArrayList<>();
        boolean success = this.detectObject(token, list, new DetectObjectListener() {
            @Override
            public void onCompleted(List<ObjectInfo> objects, long elapsed) {
                result.addAll(objects);

                synchronized (result) {
                    result.notify();
                }
            }

            @Override
            public void onFailed(List<String> fileCodes, CVStateCode stateCode) {
                synchronized (result) {
                    result.notify();
                }
            }
        });

        if (success) {
            synchronized (result) {
                try {
                    result.wait(60 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        else {
            return null;
        }

        if (result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    public boolean clipPaper(AuthToken token, List<String> fileCodes, ClipPaperListener listener) {
        final CVEndpoint endpoint = this.selectEndpoint();
        if (null == endpoint) {
            Logger.e(this.getClass(), "#clipPaper - No endpoints");
            return false;
        }

        endpoint.setWorking(true);

        final int limit = 10;
        final List<FileLabel> fileLabels = new ArrayList<>();
        for (String fileCode : fileCodes) {
            FileLabel fileLabel = this.getFile(token.getDomain(), fileCode);
            if (null != fileLabel) {
                if (fileLabel.getFileType() == FileType.JPEG ||
                        fileLabel.getFileType() == FileType.PNG ||
                        fileLabel.getFileType() == FileType.BMP) {
                    fileLabels.add(fileLabel);
                    if (fileLabels.size() >= limit) {
                        // 超限
                        break;
                    }
                }
            }
        }

        if (fileLabels.isEmpty()) {
            endpoint.setWorking(false);
            Logger.e(this.getClass(), "#clipPaper - Can NOT find files");
            return false;
        }

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONArray list = new JSONArray();
                    for (FileLabel fileLabel : fileLabels) {
                        list.put(fileLabel.toJSON());
                    }

                    JSONObject payload = new JSONObject();
                    payload.put("list", list);
                    Packet request = new Packet(CVAction.ClipPaper.name, payload);
                    ActionDialect dialect = cellet.transmit(endpoint.talkContext, request.toDialect(), 3 * 60 * 1000);
                    if (null == dialect) {
                        Logger.w(this.getClass(), "#clipPaper - Endpoint is error");
                        listener.onFailed(fileCodes, CVStateCode.EndpointException);
                        return;
                    }

                    Packet response = new Packet(dialect);
                    if (Packet.extractCode(response) != CVStateCode.Ok.code) {
                        Logger.d(this.getClass(), "#clipPaper - Process failed");
                        listener.onFailed(fileCodes, CVStateCode.Failure);
                        return;
                    }

                    List<ProcessedFile> processedFileList = new ArrayList<>();

                    JSONObject data = Packet.extractDataPayload(response);
                    JSONArray result = data.getJSONArray("result");
                    for (int i = 0; i < result.length(); ++i) {
                        ProcessedFile info = new ProcessedFile(result.getJSONObject(i));
                        Size size = getImageSize(token.getDomain(), info.getProcessed().getFileCode());
                        if (null != size) {
                            info.getProcessed().setContext(size.toJSON());
                        }
                        info.setFileLabel(findFileLabel(fileLabels, info.getFileCode()));
                        processedFileList.add(info);
                    }

                    long elapsed = data.getLong("elapsed");
                    listener.onCompleted(processedFileList, elapsed);
                    Logger.d(this.getClass(), "#clipPaper - elapsed: " + elapsed);
                } finally {
                    endpoint.setWorking(false);
                }
            }
        });

        return true;
    }

    public ProcessedFile clipPaper(AuthToken token, String fileCode) {
        List<String> list = new ArrayList<>();
        list.add(fileCode);

        final List<ProcessedFile> result = new ArrayList<>();
        boolean success = this.clipPaper(token, list, new ClipPaperListener() {
            @Override
            public void onCompleted(List<ProcessedFile> processedFiles, long elapsed) {
                result.addAll(processedFiles);

                synchronized (result) {
                    result.notify();
                }
            }

            @Override
            public void onFailed(List<String> fileCodes, CVStateCode stateCode) {
                synchronized (result) {
                    result.notify();
                }
            }
        });

        if (success) {
            synchronized (result) {
                try {
                    result.wait(60 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        else {
            return null;
        }

        if (result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    private CVEndpoint selectEndpoint() {
        synchronized (this.endpointList) {
            if (this.endpointList.isEmpty()) {
                return null;
            }

            // 先选择空闲节点
            CVEndpoint endpoint = null;
            for (CVEndpoint selected : this.endpointList) {
                if (!selected.isWorking() && selected.talkContext.isValid()) {
                    endpoint = selected;
                    break;
                }
            }

            if (null != endpoint) {
                return endpoint;
            }

            // 没有空闲，随机选择
            int index = Utils.randomInt(0, this.endpointList.size() - 1);
            endpoint = this.endpointList.get(index);
            if (!endpoint.talkContext.isValid()) {
                // 删除无效的终端节点
                this.endpointList.remove(index);
                // 校验
                if (this.endpointList.isEmpty()) {
                    return null;
                }
                else {
                    return this.endpointList.get(0);
                }
            }

            return endpoint;
        }
    }

    private FileLabel findFileLabel(List<FileLabel> fileLabels, String fileCode) {
        for (FileLabel fileLabel : fileLabels) {
            if (fileLabel.getFileCode().equals(fileCode)) {
                return fileLabel;
            }
        }
        return null;
    }

    private Size getImageSize(String domain, String fileCode) {
        File file = this.loadFile(domain, fileCode);
        if (null == file) {
            return null;
        }

        try {
            BufferedImage image = ImageIO.read(file);
            return new Size(image.getWidth(), image.getHeight());
        } catch (Exception e) {
            return null;
        }
    }
}
