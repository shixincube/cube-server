/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.contact;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.StateCode;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.state.ContactStateCode;
import cube.dispatcher.DispatcherTask;
import cube.dispatcher.Performer;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 联系人签入任务。
 */
public class SignInTask extends DispatcherTask {

    public SignInTask(ContactCellet cellet, TalkContext talkContext, Primitive primitive, Performer performer) {
        super(cellet, talkContext, primitive, performer);
    }

    @Override
    public void run() {
        String tokenCode = this.getTokenCode(this.getAction());
        if (null == tokenCode) {
            // 无令牌码
            ActionDialect response = this.makeResponse(new JSONObject(), StateCode.NoAuthToken, "No token code");
            this.cellet.speak(this.talkContext, response);
            ((ContactCellet) this.cellet).returnSignInTask(this);
            return;
        }

        Packet packet = this.getRequest();

        if (packet.data.has("code")) {
            // 仅使用令牌码签入
            ActionDialect response = this.performer.syncTransmit(this.talkContext, this.cellet.getName(), this.getAction());
            if (null == response) {
                // 发生错误
                response = this.makeResponse(packet.data, StateCode.GatewayError, "Service is disabled");
            }
            else {
                response = this.makeResponse(response);
            }

            // 对服务返回的数据进行处理，读取出其中的联系人数据
            JSONObject data = response.getParamAsJson("data");
            if (data.getInt("code") == ContactStateCode.Ok.code) {
                JSONObject contactJson = data.getJSONObject("data");
                // 将当前联系人的设备与会话上下问关联
                Contact contact = new Contact(contactJson, this.talkContext);
                Device device = contact.getDevice(this.talkContext);
                device.setToken(tokenCode);
                this.performer.updateContact(contact, device);
            }
            else {
                Logger.w(this.getClass(), "Service failed: " + data.getInt("code"));
            }

            this.cellet.speak(this.talkContext, response);
        }
        else {
            JSONObject contactJson = null;
            JSONObject authTokenJson = null;
            try {
                contactJson = packet.data.getJSONObject("self");
                authTokenJson = packet.data.getJSONObject("token");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (null == authTokenJson || null == contactJson) {
                // 没有令牌信息
                ActionDialect response = this.makeResponse(new JSONObject(), StateCode.InvalidParameter, "Parameter error");
                this.cellet.speak(this.talkContext, response);
                ((ContactCellet) this.cellet).returnSignInTask(this);
                return;
            }

            // 将当前联系人的设备与会话上下问关联
            Contact contact = new Contact(contactJson, this.talkContext);
            Device device = contact.getDevice(this.talkContext);
            device.setToken(tokenCode);

            // 判断重复登录
            Device onlineDevice = this.performer.existsDevice(device);
            if (null != onlineDevice) {
                // 通知之前的终端关闭
                JSONObject payload = new JSONObject();
                payload.put("code", ContactStateCode.DuplicateSignIn.code);
                payload.put("data", contact.toCompactJSON());
                ActionDialect response = this.makeResponse(payload, StateCode.OK, "");
                this.cellet.speak(onlineDevice.getTalkContext(), response);
            }

            this.performer.updateContact(contact, device);

            ActionDialect response = this.performer.syncTransmit(this.talkContext, this.cellet.getName(), this.getAction());
            if (null == response) {
                // 发生错误
                response = this.makeResponse(packet.data, StateCode.GatewayError, "Service is disabled");
            }
            else {
                response = this.makeResponse(response);
            }

            this.cellet.speak(this.talkContext, response);
        }

        ((ContactCellet)this.cellet).returnSignInTask(this);
    }
}
