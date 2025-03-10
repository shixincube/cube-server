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
import cube.common.entity.*;
import cube.common.state.ContactStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;

/**
 * 获取联系人所在的所有群。
 */
public class ListGroupsTask extends ServiceTask {

    public ListGroupsTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
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
            Logger.w(this.getClass(), "Can NOT find contact with token: " + tokenCode);
            markResponseTime();
            return;
        }

        // 发起查询的设备
        Device device = ContactManager.getInstance().getDevice(tokenCode);
        // 域
        String domain = contact.getDomain().getName();

        // 获取查询起始时间
        long beginning = 0;
        long ending = 0;
        int pageSize = 4;
        GroupState groupState = GroupState.Normal;
        try {
            beginning = data.getLong("beginning");

            if (data.has("ending")) {
                ending = data.getLong("ending");
            }

            // 指定群组状态
            if (data.has("state")) {
                groupState = GroupState.parse(data.getInt("state"));
            }

            if (data.has("pageSize")) {
                pageSize = data.getInt("pageSize");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (ending == 0) {
            ending = System.currentTimeMillis();
        }

        // 查询从指定活跃时间之后的该联系人所在的所有群
        List<Group> list = ContactManager.getInstance().listGroupsWithMember(domain,
                contact.getId(), beginning, ending, groupState);

        // 排除标签
        Iterator<Group> iter = list.iterator();
        while (iter.hasNext()) {
            Group group = iter.next();
            if (!group.getTag().equals(GroupTag.Public)) {
                iter.remove();
            }
        }

        if (list.isEmpty()) {
            JSONObject responseData = new JSONObject();
            responseData.put("beginning", beginning);
            responseData.put("ending", ending);
            responseData.put("total", list.size());
            responseData.put("list", new JSONArray());
            this.cellet.speak(this.talkContext,
                    this.makeAsynResponse(packet, contact.getId(), domain, device,
                            ContactStateCode.Ok.code, responseData));
            markResponseTime();
            return;
        }

        int total = list.size();

        while (!list.isEmpty()) {
            JSONObject responseData = new JSONObject();
            responseData.put("beginning", beginning);
            responseData.put("ending", ending);
            responseData.put("total", total);

            JSONArray array = new JSONArray();
            for (int i = 0; i < pageSize; ++i) {
                Group group = list.remove(0);
                array.put(group.toJSON());
                if (list.isEmpty()) {
                    break;
                }
            }

            responseData.put("list", array);

            this.cellet.speak(this.talkContext,
                    this.makeAsynResponse(packet, contact.getId(), domain, device,
                            ContactStateCode.Ok.code, responseData));
        }

        markResponseTime();
    }
}
