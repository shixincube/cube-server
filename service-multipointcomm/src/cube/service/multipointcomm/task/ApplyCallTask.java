/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

package cube.service.multipointcomm.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.log.Logger;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.CommField;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.state.MultipointCommStateCode;
import cube.service.ServiceTask;
import cube.service.multipointcomm.MultipointCommService;
import org.json.JSONException;

/**
 * Apply Call 任务。
 */
public class ApplyCallTask extends ServiceTask {

    public ApplyCallTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        CommField field = null;
        Contact participant = null;
        Device device = null;

        try {
            field = new CommField(packet.data.getJSONObject("field"));
            participant = new Contact(packet.data.getJSONObject("participant"));
            device = new Device(packet.data.getJSONObject("device"));
        } catch (JSONException e) {
            Logger.w(this.getClass(), "#run", e);
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, MultipointCommStateCode.InvalidParameter.code, packet.data));
            markResponseTime();
            return;
        }

        MultipointCommService service = (MultipointCommService) this.kernel.getModule(MultipointCommService.NAME);

        // 申请进行通话
        MultipointCommStateCode state = service.applyCall(field, participant, device);
        if (state != MultipointCommStateCode.Ok) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, state.code, packet.data));
            markResponseTime();
            return;
        }

        // 向客户端更新数据
        CommField current = service.getCommField(field.getId());

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, state.code, current.toJSON()));
        markResponseTime();
    }
}
