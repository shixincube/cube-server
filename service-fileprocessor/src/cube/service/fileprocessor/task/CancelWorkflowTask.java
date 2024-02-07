/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

package cube.service.fileprocessor.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.state.FileProcessorStateCode;
import cube.file.OperationWorkflow;
import cube.service.ServiceTask;
import cube.service.fileprocessor.FileProcessorService;
import cube.service.fileprocessor.FileProcessorServiceCellet;
import org.json.JSONObject;

/**
 * 取消工作流任务。
 */
public class CancelWorkflowTask extends ServiceTask {

    /**
     * 构造函数。
     *
     * @param cellet
     * @param talkContext
     * @param primitive
     * @param responseTime
     */
    public CancelWorkflowTask(Cellet cellet, TalkContext talkContext, Primitive primitive,
                              ResponseTime responseTime) {
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
                    this.makeResponse(action, packet, FileProcessorStateCode.Unauthorized.code, packet.data));
            markResponseTime();
            return;
        }

        // 请求的数据
        JSONObject data = packet.data;
        JSONObject workflowJson = data.getJSONObject("workflow");

        OperationWorkflow workflow = new OperationWorkflow(workflowJson);

        FileProcessorService service = ((FileProcessorServiceCellet) this.cellet).getService();

        markResponseTime();
    }
}
