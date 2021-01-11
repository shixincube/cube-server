/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

package cube.service.contact.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
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

    public AddGroupMemberTask(Cellet cellet, TalkContext talkContext, Primitive primitive) {
        super(cellet, talkContext, primitive);
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
            e.printStackTrace();
        }

        // 判读参数列表里的成员数组是否为空
        if (null != memberList && memberList.isEmpty()) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.IllegalOperation.code, data));
            return;
        }
        else if (null != memberIdList && memberIdList.isEmpty()) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.IllegalOperation.code, data));
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
            return;
        }

        // 返回的数据负载
        JSONObject payload = bundle.toJSON();

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, ContactStateCode.Ok.code, payload));
    }
}
