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
import cell.util.log.Logger;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.entity.SharingTag;
import cube.common.state.FileStorageStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import cube.service.filestorage.FileStorageService;
import cube.service.filestorage.FileStorageServiceCellet;

/**
 * 创建分享标签任务。
 */
public class CreateSharingTagTask extends ServiceTask {

    public CreateSharingTagTask(FileStorageServiceCellet cellet, TalkContext talkContext,
                                Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        // 获取令牌码
        String tokenCode = this.getTokenCode(action);
        if (null == tokenCode) {
            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Unauthorized.code, packet.data));
            markResponseTime();
            return;
        }

        Contact contact = ContactManager.getInstance().getContact(tokenCode);
        Device device = ContactManager.getInstance().getDevice(tokenCode);
        if (null == contact || null == device) {
            if (null == device) {
                Logger.w(CreateSharingTagTask.class, "Device is null, token: " + tokenCode);
            }

            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.InvalidDomain.code, packet.data));
            markResponseTime();
            return;
        }

        // 读取参数
        String fileCode = packet.data.getString("fileCode");
        long duration = packet.data.has("duration") ? packet.data.getLong("duration") : 0;
        String password = packet.data.has("password") ? packet.data.getString("password") : null;
        boolean preview = packet.data.has("preview") ? packet.data.getBoolean("preview") : false;
        String previewWatermark = packet.data.has("watermark") ? packet.data.getString("watermark") : null;
        boolean download = packet.data.has("download") ? packet.data.getBoolean("download") : true;
        boolean traceDownload = packet.data.has("traceDownload") ? packet.data.getBoolean("traceDownload") : true;

        FileStorageService service = (FileStorageService) this.kernel.getModule(FileStorageService.NAME);

        // 创建分享标签
        SharingTag sharingTag = service.getSharingManager().createSharingTag(contact, device,
                fileCode, duration, password, preview, previewWatermark, download, traceDownload);
        if (null == sharingTag) {
            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Failure.code, packet.data));
            markResponseTime();
            return;
        }

        // 返回数据
        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, FileStorageStateCode.Ok.code, sharingTag.toCompactJSON()));
        markResponseTime();
    }
}
