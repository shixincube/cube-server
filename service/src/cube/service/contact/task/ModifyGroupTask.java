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
import cube.common.entity.Group;
import cube.common.state.ContactStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 修改群组信息任务。
 */
public class ModifyGroupTask extends ServiceTask {

    public ModifyGroupTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        JSONObject data = packet.data;

        String tokenCode = this.getTokenCode(action);
        Contact contact = ContactManager.getInstance().getContact(tokenCode);
        if (null == contact) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.NoSignIn.code, data));
            markResponseTime();
            return;
        }

        // 域
        String domain = contact.getDomain().getName();

        Long groupId = null;
        Long newOwnerId = null;
        Contact newOwner = null;
        String newName = null;
        JSONObject newContext = null;
        try {
            // 群组 ID
            groupId = data.has("id") ? data.getLong("id") : data.getLong("groupId");

            if (data.has("owner")) {
                newOwner = new Contact(data.getJSONObject("owner"), domain);
            }
            if (data.has("ownerId")) {
                newOwnerId = data.getLong("ownerId");
            }
            if (data.has("name")) {
                newName = data.getString("name");
            }
            if (data.has("context")) {
                newContext = data.getJSONObject("context");
            }
        } catch (JSONException e) {
            Logger.w(this.getClass(), "#run", e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.InvalidParameter.code, data));
            markResponseTime();
            return;
        }

        // 获取群组
        Group group = ContactManager.getInstance().getGroup(groupId, domain);
        if (null == group) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.Failure.code, data));
            markResponseTime();
            return;
        }

        if (null != newOwner) {
            // 新的群组必须是当前的群成员
            if (group.hasMember(newOwner.getId())) {
                group.setOwnerId(newOwner.getId());
            }
        }
        else if (null != newOwnerId) {
            if (group.hasMember(newOwnerId)) {
                group.setOwnerId(newOwnerId);
            }
        }

        if (null != newName) {
            group.setName(newName);
        }
        if (null != newContext) {
            group.setContext(newContext);
        }

        // 修改群组信息
        Group result = ContactManager.getInstance().modifyGroup(group);

        // 返回 Compact 结构
        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, ContactStateCode.Ok.code, result.toCompactJSON()));
        markResponseTime();
    }
}
