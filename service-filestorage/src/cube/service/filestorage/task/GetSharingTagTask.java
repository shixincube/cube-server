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
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.SharingTag;
import cube.common.state.FileStorageStateCode;
import cube.service.ServiceTask;
import cube.service.filestorage.FileStorageService;
import cube.service.filestorage.FileStorageServiceCellet;

/**
 * 获取分享标签任务。
 */
public class GetSharingTagTask extends ServiceTask {

    public GetSharingTagTask(FileStorageServiceCellet cellet, TalkContext talkContext,
                             Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        // 读取参数
        String sharingCode = packet.data.getString("code");
        boolean refresh = packet.data.has("refresh") && packet.data.getBoolean("refresh");
        boolean full = packet.data.has("full") && packet.data.getBoolean("full");

        FileStorageService service = (FileStorageService) this.kernel.getModule(FileStorageService.NAME);
        SharingTag sharingTag = service.getSharingManager().getSharingTag(sharingCode, refresh);
        if (null == sharingTag) {
            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Failure.code, packet.data));
            markResponseTime();
            return;
        }

        // 返回数据
        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, FileStorageStateCode.Ok.code,
                        full ? sharingTag.toJSON() : sharingTag.toCompactJSON()));
        markResponseTime();
    }
}
