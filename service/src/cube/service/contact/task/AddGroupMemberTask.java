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
import cube.service.contact.GroupBundle;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 添加群成员任务。
 */
public class AddGroupMemberTask extends ServiceTask {

    public AddGroupMemberTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
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

        // 域
        String domain = contact.getDomain().getName();

        Long groupId = null;
        List<Long> memberIdList = null;
        List<Contact> memberList = null;
        Contact operator = null;
        try {
            groupId = data.getLong("groupId");

            if (data.has("memberIdList")) {
                memberIdList = new ArrayList<>();

                JSONArray list = data.getJSONArray("memberIdList");
                for (int i = 0; i < list.length(); ++i) {
                    memberIdList.add(list.getLong(i));
                }
            }
            else {
                memberList = new ArrayList<>();

                JSONArray list = data.getJSONArray("memberList");
                for (int i = 0; i < list.length(); ++i) {
                    JSONObject json = list.getJSONObject(i);
                    Contact member = new Contact(json, domain);
                    memberList.add(member);
                }
            }

            JSONObject operatorJson = data.getJSONObject("operator");
            operator = new Contact(operatorJson, domain);
        } catch (JSONException e) {
            Logger.w(this.getClass(), "#run", e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.InvalidParameter.code, data));
            markResponseTime();
            return;
        }

        // 判断参数列表里的成员数组是否为空
        if (null != memberList && memberList.isEmpty()) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.IllegalOperation.code, data));
            markResponseTime();
            return;
        }
        else if (null != memberIdList && memberIdList.isEmpty()) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.IllegalOperation.code, data));
            markResponseTime();
            return;
        }

        GroupBundle bundle = null;
        if (null != memberList) {
            bundle = ContactManager.getInstance().addGroupMembers(domain, groupId, memberList, operator);
        }
        else {
            bundle = ContactManager.getInstance().addGroupMembersById(domain, groupId, memberIdList, operator);
        }

        if (null == bundle) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.Failure.code, data));
            markResponseTime();
            return;
        }

        // 返回的数据负载
        JSONObject payload = bundle.toJSON();

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, ContactStateCode.Ok.code, payload));
        markResponseTime();
    }
}
