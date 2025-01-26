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
import cube.common.entity.FileLabel;
import cube.common.state.FileStorageStateCode;
import cube.service.ServiceTask;
import cube.service.filestorage.FileStorageService;
import cube.service.filestorage.FileStorageServiceCellet;

/**
 * 放置文件任务。
 */
public class PutFileTask extends ServiceTask {

    public PutFileTask(FileStorageServiceCellet cellet, TalkContext talkContext,
                       Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        FileLabel fileLabel = new FileLabel(packet.data);

        FileStorageService service = (FileStorageService) this.kernel.getModule(FileStorageService.NAME);

        // 放置文件
        FileLabel newFileLabel = service.putFile(fileLabel);
        if (null == newFileLabel) {
            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Failure.code, fileLabel.toCompactJSON()));
            markResponseTime();
            return;
        }

        // 应答
        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, FileStorageStateCode.Ok.code, newFileLabel.toCompactJSON()));
        markResponseTime();
    }
}
