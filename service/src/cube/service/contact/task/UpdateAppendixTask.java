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
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.Contact;
import cube.common.entity.ContactAppendix;
import cube.common.entity.Group;
import cube.common.entity.GroupAppendix;
import cube.common.state.ContactStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 更新附录任务。
 */
public class UpdateAppendixTask extends ServiceTask {

    public UpdateAppendixTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
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

        if (data.has("contactId")) {
            Long id = data.getLong("contactId");
            Contact target = ContactManager.getInstance().getContact(contact.getDomain().getName(), id);

            // 获取附录
            ContactAppendix appendix = ContactManager.getInstance().getAppendix(target);

            // 更新备注名
            if (data.has("remarkName")) {
                String remarkName = data.getString("remarkName");
                appendix.remarkName(contact, remarkName);
            }

            if (id.longValue() == contact.getId().longValue()) {
                // 本联系人提交的更新数据
                if (data.has("assignedData")) {
                    // 更新全部赋值数据
                    JSONObject map = data.getJSONObject("assignedData");
                    for (String key : map.keySet()) {
                        appendix.setAssignedData(key, map.getJSONObject(key));
                    }
                }
                else if (data.has("assignedKey") && data.has("assignedValue")) {
                    // 更新指定键值数据
                    try {
                        String key = data.getString("assignedKey");
                        JSONObject value = data.getJSONObject("assignedValue");
                        appendix.setAssignedData(key, value);
                    } catch (JSONException e) {
                        this.cellet.speak(this.talkContext,
                                this.makeResponse(action, packet, ContactStateCode.DataStructureError.code, data));
                        markResponseTime();
                        return;
                    }
                }
            }

            ContactManager.getInstance().updateAppendix(appendix);

            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.Ok.code, appendix.packJSON(contact)));
        }
        else if (data.has("groupId")) {
            Long groupId = data.getLong("groupId");
            Group group = ContactManager.getInstance().getGroup(groupId, contact.getDomain().getName());
            if (null == group) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(action, packet, ContactStateCode.NotFindGroup.code, data));
                markResponseTime();
                return;
            }

            // 判断是否是群组成员
            if (!group.hasMember(contact.getId())) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(action, packet, ContactStateCode.IllegalOperation.code, data));
                markResponseTime();
                return;
            }

            // 获取附录
            GroupAppendix appendix = ContactManager.getInstance().getAppendix(group);

            boolean broadcast = false;
            boolean modified = false;
            boolean needResetTimestamp = false;

            if (data.has("notice")) {
                // 更新公告
                modified = appendix.setNotice(data.getString("notice"), contact);
                needResetTimestamp = true;
            }

            if (data.has("memberRemark")) {
                // 更新群成员备注名
                JSONObject remark = data.getJSONObject("memberRemark");
                appendix.setMemberRemarkName(remark.getLong("id"), remark.getString("name"));
                modified = true;
            }

            if (data.has("remark")) {
                // 更新成员对该群的备注
                appendix.remarkGroup(contact, data.getString("remark"));
                modified = true;
            }

            if (data.has("following")) {
                // 更新关注
                appendix.setFollowing(contact, data.getBoolean("following"));
                modified = true;
            }

            if (data.has("memberNameDisplayed")) {
                // 更新群成员名称显示标志位
                appendix.setMemberNameDisplayed(contact, data.getBoolean("memberNameDisplayed"));
                modified = true;
            }

            if (data.has("commId")) {
                // 更新群组当前的通讯 ID
                Long commId = data.getLong("commId");
                // 判断 Comm ID 是否改变，如果改变需要进行广播
                Long cur = appendix.getCommId();
                if (cur.longValue() != commId.longValue()) {
                    appendix.setCommId(commId);
                    broadcast = true;
                }
                modified = true;
            }

            if (modified) {
                if (needResetTimestamp) {
                    // 更新时间戳
                    group.resetTimestamp();
                }

                ContactManager.getInstance().updateAppendix(group, appendix, broadcast);
            }

            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.Ok.code, appendix.packJSON(contact.getId())));
        }
        else {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.Failure.code, data));
        }

        markResponseTime();
    }
}
