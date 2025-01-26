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
import cube.common.entity.VisitTrace;
import cube.common.state.FileStorageStateCode;
import cube.service.ServiceTask;
import cube.service.filestorage.FileStorageService;
import cube.service.filestorage.FileStorageServiceCellet;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * 追踪任务。
 */
public class TraceTask extends ServiceTask {

    public TraceTask(FileStorageServiceCellet cellet, TalkContext talkContext,
                     Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        VisitTrace visitTrace = new VisitTrace(packet.data);

        try {
            URI uri = new URI(visitTrace.url);
            String path = uri.getPath();

            // 通过 path 判断行为
            if (path.startsWith("/sharing")) {
                FileStorageService service = (FileStorageService) this.kernel.getModule(FileStorageService.NAME);
                service.getSharingManager().traceVisit(visitTrace);
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        JSONObject data = new JSONObject();
        data.put("time", visitTrace.time);

        // 应答
        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, FileStorageStateCode.Ok.code, data));
        markResponseTime();
    }
}
