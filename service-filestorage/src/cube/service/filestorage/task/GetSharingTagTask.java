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
