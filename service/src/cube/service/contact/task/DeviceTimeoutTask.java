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

package cube.service.contact.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.Task;
import cube.common.entity.Device;
import cube.service.contact.ContactManager;

/**
 * 设备超时任务。
 */
public class DeviceTimeoutTask extends Task {

    public DeviceTimeoutTask(Cellet cellet, TalkContext talkContext, Primitive primitive) {
        super(cellet, talkContext, primitive);
    }

    @Override
    public void run() {
        ActionDialect dialect = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        Long contactId = null;
        Device device = null;
        long failureTime = 0;
        long timeout = 0;

        JSONObject data = packet.data;
        try {
            contactId = data.getLong("id");
            JSONObject deviceJson = data.getJSONObject("device");
            failureTime = data.getLong("failureTime");
            timeout = data.getLong("timeout");
            device = new Device(deviceJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // 移除联系人的设备
        ContactManager.getInstance().removeContactDevice(contactId, device);
    }
}
