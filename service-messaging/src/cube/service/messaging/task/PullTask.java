/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

package cube.service.messaging.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.json.JSONArray;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cube.common.Domain;
import cube.common.Packet;
import cube.common.action.MessagingActions;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.entity.Message;
import cube.common.state.MessagingStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import cube.service.messaging.MessagingManager;

import java.util.List;

/**
 * 拉取消息任务。
 */
public class PullTask extends ServiceTask {

    private static long AWEEK = 7L * 24L * 60L * 60L * 1000L;

    public PullTask(Cellet cellet, TalkContext talkContext, Primitive primitive) {
        super(cellet, talkContext, primitive);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        Long id = null;
        String domainName = null;
        Device device = null;
        long timestamp = 0;
        try {
            id = packet.data.getLong("id");
            domainName = packet.data.getString("domain");
            JSONObject dev = packet.data.getJSONObject("device");
            device = new Device(dev);

            if (packet.data.has("timestamp")) {
                timestamp = packet.data.getLong("timestamp");
            }
        } catch (JSONException e) {
            // 应答
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MessagingStateCode.NoDevice.code, packet.data));
            return;
        }

        // 获取联系人
        Contact contact = ContactManager.getInstance().getOnlineContact(new Domain(domainName), id);
        if (null == contact) {
            // 应答
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MessagingStateCode.NoContact.code, packet.data));
            return;
        }

        // 检查设备是否属于该联系人
        if (!contact.hasDevice(device)) {
            // 应答
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MessagingStateCode.Failure.code, packet.data));
            return;
        }

        // 计算起始时间
        long now = System.currentTimeMillis();
        if (timestamp == 0) {
            timestamp = now - AWEEK;
        }

        // 获取指定起始时间的消息列表
        List<Message> messageList = MessagingManager.getInstance().pullMessage(id, timestamp, now);
        int total = messageList.size();
        int count = 0;
        JSONArray messageArray = new JSONArray();

        while (!messageList.isEmpty()) {
            Message message = messageList.remove(0);
            messageArray.put(message.toJSON());

            ++count;
            if (count >= 10) {
                JSONObject payload = this.makePayload(total, timestamp, now, messageArray);
                this.cellet.speak(this.talkContext,
                        this.makeResponse(action, packet, MessagingStateCode.Ok.code, payload));

                try {
                    Thread.sleep(10L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                count = 0;
                messageArray = new JSONArray();
            }
        }

        JSONObject payload = makePayload(total, timestamp, now, messageArray);
        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, MessagingStateCode.Ok.code, payload));
    }

    private JSONObject makePayload(int totalNum, long beginning, long ending, JSONArray messages) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("total", totalNum);
            payload.put("beginning", beginning);
            payload.put("ending", ending);
            payload.put("messages", messages);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }
}
