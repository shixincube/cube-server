/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
