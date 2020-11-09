/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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
import cell.util.json.JSONException;
import cube.common.Packet;
import cube.common.entity.FileLabel;
import cube.common.state.FileStorageStateCode;
import cube.service.ServiceTask;
import cube.service.filestorage.FileStorageService;
import cube.service.filestorage.FileStorageServiceCellet;

/**
 * 获取文件任务。
 */
public class GetFileTask extends ServiceTask {

    public GetFileTask(FileStorageServiceCellet cellet, TalkContext talkContext, Primitive primitive) {
        super(cellet, talkContext, primitive);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        String fileCode = null;
        try {
            fileCode = packet.data.getString("fileCode");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        FileStorageService service = (FileStorageService) this.kernel.getModule(FileStorageServiceCellet.NAME);

        // 放置文件
        FileLabel fileLabel = service.getFile(fileCode);
        if (null == fileLabel) {
            // 发生错误
            this.cellet.speak(this.talkContext
                    , this.makeResponse(action, packet, FileStorageStateCode.Failure.code, packet.data));
            return;
        }

        // 应答
        this.cellet.speak(this.talkContext
                , this.makeResponse(action, packet, FileStorageStateCode.Ok.code, fileLabel.toJSON()));
    }
}
