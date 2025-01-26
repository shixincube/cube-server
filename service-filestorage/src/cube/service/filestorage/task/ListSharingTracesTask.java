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
import cube.common.entity.Contact;
import cube.common.entity.TransmissionChain;
import cube.common.entity.VisitTrace;
import cube.common.state.FileStorageStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import cube.service.filestorage.FileStorageService;
import cube.service.filestorage.FileStorageServiceCellet;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 列表分享标签访问记录。
 */
public class ListSharingTracesTask extends ServiceTask {

    public ListSharingTracesTask(FileStorageServiceCellet cellet, TalkContext talkContext,
                                 Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        if (!packet.data.has("sharingCode")) {
            // 参数错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Forbidden.code, packet.data));
            markResponseTime();
            return;
        }

        // 获取令牌码
        String tokenCode = this.getTokenCode(action);
        Contact contact = ContactManager.getInstance().getContact(tokenCode);
        if (null == contact) {
            // 发生错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Unauthorized.code, packet.data));
            markResponseTime();
            return;
        }

        String sharingCode = packet.data.getString("sharingCode");

        if (packet.data.has("begin") && packet.data.has("end")) {
            int beginIndex = packet.data.getInt("begin");
            int endIndex = packet.data.getInt("end");

            // 校验参数
            int d = endIndex - beginIndex;
            if (d > 9) {
                endIndex = beginIndex + 9;
            }

            // 获取服务
            FileStorageService service = (FileStorageService) this.kernel.getModule(FileStorageService.NAME);
            List<VisitTrace> list = service.getSharingManager().listSharingVisitTrace(contact, sharingCode, beginIndex, endIndex);
            if (null == list) {
                // 发生错误
                this.cellet.speak(this.talkContext,
                        this.makeResponse(action, packet, FileStorageStateCode.Forbidden.code, packet.data));
                markResponseTime();
                return;
            }

            JSONArray array = new JSONArray();
            for (VisitTrace trace : list) {
                array.put(trace.toCompactJSON());
            }

            JSONObject result = new JSONObject();
            result.put("list", array);
            result.put("total", service.getSharingManager().countSharingVisitTrace(contact, sharingCode));
            result.put("begin", beginIndex);
            result.put("end", endIndex);
            result.put("sharingCode", sharingCode);

            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Ok.code, result));
            markResponseTime();
        }
        else if (packet.data.has("trace")) {
            // 追踪深度
            int trace = packet.data.getInt("trace");

            // 获取服务
            FileStorageService service = (FileStorageService) this.kernel.getModule(FileStorageService.NAME);
            TransmissionChain chain = service.getSharingManager().calcTraceChain(contact, sharingCode, trace);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Ok.code, chain.toCompactJSON()));
            markResponseTime();
        }
        else {
            // 参数错误
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Forbidden.code, packet.data));
            markResponseTime();
        }
    }
}
