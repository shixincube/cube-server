/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.contact.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.log.Logger;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.Contact;
import cube.common.state.ContactStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * 置顶任务。
 */
public class TopListTask extends ServiceTask {

    public TopListTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        JSONObject data = packet.data;

        String tokenCode = this.getTokenCode(action);
        if (null == tokenCode) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.InvalidParameter.code, data));
            markResponseTime();
            return;
        }

        Contact contact = ContactManager.getInstance().getContact(tokenCode);
        if (null == contact) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.NoSignIn.code, data));
            markResponseTime();
            return;
        }

        String topAction = null;
        Long topId = null;
        String type = null;
        try {
            topAction = data.getString("action");
            if (data.has("topId")) {
                topId = data.getLong("topId");
            }
            if (data.has("type")) {
                type = data.getString("type");
            }
        } catch (JSONException e) {
            Logger.w(this.getClass(), "#run", e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.InvalidParameter.code, data));
            markResponseTime();
            return;
        }

        if (topAction.equals("get")) {
            // 获取置顶列表
            List<JSONObject> list = ContactManager.getInstance().getTopList(contact);

            JSONArray array = new JSONArray();
            for (JSONObject json : list) {
                array.put(json);
            }

            JSONObject json = new JSONObject();
            json.put("action", topAction);
            json.put("list", array);

            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.Ok.code, json));
        }
        else if (topAction.equals("add")) {
            // 添加置顶 ID
            ContactManager.getInstance().addTopList(contact, topId, type);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.Ok.code, data));
        }
        else if (topAction.equals("remove")) {
            // 移除置顶 ID
            ContactManager.getInstance().removeTopList(contact, topId);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.Ok.code, data));
        }

        markResponseTime();
    }
}
