/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2020-2025 Ambrose Xu.
 */

package cube.service.cv;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.common.Packet;
import cube.common.action.CVAction;
import cube.common.entity.BarCode;
import cube.common.entity.BarCodeInfo;
import cube.common.entity.Contact;
import cube.common.entity.FileLabel;
import cube.common.notice.GetFile;
import cube.common.state.AIGCStateCode;
import cube.common.state.CVStateCode;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.plugin.PluginSystem;
import cube.service.auth.AuthService;
import cube.service.cv.listener.DetectBarCodeListener;
import cube.util.FileType;
import org.json.JSONArray;
import org.json.JSONObject;

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
        this.executor = Executors.newCachedThreadPool();
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
            Logger.e(this.getClass(), "#detectBarCode - Can NOT find files");
            return false;
        }

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
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
                    }

                    BarCodeInfo info = new BarCodeInfo(fileLabel, codeList);
                    infoList.add(info);
                }

                Logger.d(this.getClass(), "#detectBarCode - result: " + infoList.size());
                listener.onCompleted(infoList);
            }
        });

        return true;
    }

    private CVEndpoint selectEndpoint() {
        synchronized (this.endpointList) {
            if (this.endpointList.isEmpty()) {
                return null;
            }

            int index = Utils.randomInt(0, this.endpointList.size() - 1);
            CVEndpoint endpoint = this.endpointList.get(index);
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
}
