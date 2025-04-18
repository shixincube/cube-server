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
import cube.common.entity.SharingTag;
import cube.common.state.FileStorageStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import cube.service.filestorage.FileStorageService;
import cube.service.filestorage.FileStorageServiceCellet;
import cube.util.ConfigUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 列举分享标签任务。
 */
public class ListSharingTagsTask extends ServiceTask {

    public ListSharingTagsTask(FileStorageServiceCellet cellet, TalkContext talkContext,
                               Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        if (!packet.data.has("begin") || !packet.data.has("end")) {
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

        int beginIndex = packet.data.getInt("begin");
        int endIndex = packet.data.getInt("end");
        boolean valid = packet.data.has("valid") && packet.data.getBoolean("valid");
        boolean desc = !packet.data.has("order") || packet.data.getString("order").equalsIgnoreCase(ConfigUtils.ORDER_DESC);

        // 校验参数
        int d = endIndex - beginIndex;
        if (d > 9) {
            endIndex = beginIndex + 9;
        }

        // 获取服务
        FileStorageService service = (FileStorageService) this.kernel.getModule(FileStorageService.NAME);
        try {
            List<SharingTag> list = service.getSharingManager().listSharingTags(contact, valid, beginIndex, endIndex, desc);
            if (null == list) {
                // 发生错误
                this.cellet.speak(this.talkContext,
                        this.makeResponse(action, packet, FileStorageStateCode.Forbidden.code, packet.data));
                markResponseTime();
                return;
            }

            JSONArray array = new JSONArray();
            for (SharingTag sharingTag : list) {
                array.put(sharingTag.toCompactJSON());
            }

            JSONObject result = new JSONObject();
            result.put("list", array);
            result.put("total", service.getSharingManager().countSharingTags(contact, valid));
            result.put("begin", beginIndex);
            result.put("end", endIndex);
            result.put("valid", valid);

            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Ok.code, result));
            markResponseTime();
        } catch (Exception e) {
            Logger.e(this.getClass(), "List sharing tag", e);

            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FileStorageStateCode.Failure.code, packet.data));
            markResponseTime();
        }
    }
}
