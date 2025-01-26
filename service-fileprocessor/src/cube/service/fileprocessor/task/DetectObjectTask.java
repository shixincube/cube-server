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
import cube.common.action.FileProcessorAction;
import cube.common.state.FileProcessorStateCode;
import cube.service.ServiceTask;
import cube.service.fileprocessor.CVResult;
import cube.service.fileprocessor.FileProcessorService;
import cube.service.fileprocessor.FileProcessorServiceCellet;
import org.json.JSONObject;

/**
 * 检测图像对象任务。
 */
public class DetectObjectTask extends ServiceTask {

    /**
     * 构造函数。
     *
     * @param cellet
     * @param talkContext
     * @param primitive
     */
    public DetectObjectTask(Cellet cellet, TalkContext talkContext, Primitive primitive,
                            ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        JSONObject data = packet.data;

        String domain = data.getString("domain");
        String fileCode = data.getString("fileCode");

        FileProcessorService service = ((FileProcessorServiceCellet) this.cellet).getService();

        // 进行识别
        CVResult result = service.detectObject(domain, fileCode);
        if (null == result) {
            // 应答
            ActionDialect response = this.makeResponse(action, packet,
                    FileProcessorAction.DetectObjectAck.name, FileProcessorStateCode.Failure.code, data);
            this.cellet.speak(this.talkContext, response);
            markResponseTime();
            return;
        }

        // 应答
        ActionDialect response = this.makeResponse(action, packet,
                FileProcessorAction.DetectObjectAck.name, FileProcessorStateCode.Ok.code, result.toJSON());
        this.cellet.speak(this.talkContext, response);
        markResponseTime();
    }
}
