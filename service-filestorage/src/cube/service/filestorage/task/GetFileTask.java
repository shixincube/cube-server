/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.filestorage.task;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.entity.FileLabel;
import cube.common.state.FileStorageStateCode;
import cube.file.hook.FileStorageHook;
import cube.service.ServiceTask;
import cube.service.auth.AuthService;
import cube.service.contact.ContactManager;
import cube.service.filestorage.FileStoragePluginContext;
import cube.service.filestorage.FileStorageService;
import cube.service.filestorage.FileStorageServiceCellet;

/**
 * 获取文件任务。
 */
public class GetFileTask extends ServiceTask {

    public GetFileTask(FileStorageServiceCellet cellet, TalkContext talkContext,
                       Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        // 域
        String domain = null;

        String tokenCode = null;

        if (action.containsParam("token")) {
            // 获取令牌码
            tokenCode = this.getTokenCode(action);
            if (null == tokenCode) {
                // 发生错误
                this.cellet.speak(this.talkContext,
                        this.makeResponse(action, packet, FileStorageStateCode.Unauthorized.code, packet.data));
                markResponseTime();
                return;
            }

            AuthService authService = (AuthService) this.kernel.getModule(AuthService.NAME);
            AuthToken authToken = authService.getToken(tokenCode);
            if (null == authToken) {
                // 发生错误
                this.cellet.speak(this.talkContext,
                        this.makeResponse(action, packet, FileStorageStateCode.InvalidDomain.code, packet.data));
                markResponseTime();
                return;
            }

            // 域
            domain = authToken.getDomain();
        }
        else {
            if (!packet.data.has("domain")) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(action, packet, FileStorageStateCode.Failure.code, packet.data));
                markResponseTime();
                return;
            }

            domain = packet.data.getString("domain");
        }

        String fileCode = null;
        try {
            fileCode = packet.data.getString("fileCode");
        } catch (Exception e) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Failure.code, packet.data));
            markResponseTime();
            return;
        }

        FileStorageService service = (FileStorageService) this.kernel.getModule(FileStorageService.NAME);

        // 放置文件
        FileLabel fileLabel = service.getFile(domain, fileCode);
        if (null == fileLabel) {
            // 判断是否正在放置文件
            boolean hasDescriptor = service.hasFileDescriptor(fileCode);

            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet,
                            hasDescriptor ? FileStorageStateCode.Writing.code : FileStorageStateCode.NotFound.code,
                            packet.data));
            markResponseTime();
            return;
        }

        // 应答
        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, FileStorageStateCode.Ok.code, fileLabel.toJSON()));
        markResponseTime();

        // 如果有设备信息记录下载事件
        String deviceName = action.containsParam("device") ?
                action.getParamAsString("device") : null;
        if (null != deviceName && null != tokenCode) {
            recordDownload(service, tokenCode, fileLabel);
        }
    }

    private void recordDownload(FileStorageService service, String tokenCode,
                                FileLabel fileLabel) {
        Device device = ContactManager.getInstance().getDevice(tokenCode);
        Contact contact = ContactManager.getInstance().getContact(tokenCode);

        if (null != contact && null != device) {
            FileStorageHook hook = service.getPluginSystem().getDownloadFileHook();
            hook.apply(new FileStoragePluginContext(fileLabel, contact, device));
        }
    }
}
