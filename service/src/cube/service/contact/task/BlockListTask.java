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
 * 阻止清单操作任务。
 */
public class BlockListTask extends ServiceTask {

    public BlockListTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
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

        String blockAction = null;
        Long blockId = null;
        try {
            blockAction = data.getString("action");
            if (data.has("blockId")) {
                blockId = data.getLong("blockId");
            }
        } catch (JSONException e) {
            Logger.w(this.getClass(), "#run", e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.InvalidParameter.code, data));
            markResponseTime();
            return;
        }

        List<Long> list = null;

        if (blockAction.equals("get")) {
            // 获取阻止列表
            list = ContactManager.getInstance().getBlockList(contact);
        }
        else if (blockAction.equals("add")) {
            // 添加被阻止的联系人 ID
            list = ContactManager.getInstance().addBlockList(contact, blockId);
        }
        else if (blockAction.equals("remove")) {
            // 移除被阻止的联系人 ID
            list = ContactManager.getInstance().removeBlockList(contact, blockId);
        }
        else {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.InvalidParameter.code, data));
            markResponseTime();
            return;
        }

        JSONArray array = new JSONArray();
        for (Long id : list) {
            array.put(id.longValue());
        }

        JSONObject json = new JSONObject();
        json.put("action", blockAction);
        json.put("list", array);
        if (null != blockId) {
            json.put("blockId", blockId.longValue());
        }

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, ContactStateCode.Ok.code, json));

        markResponseTime();
    }
}
