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
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.action.MultipointCommAction;
import cube.common.state.MultipointCommStateCode;
import cube.service.ServiceTask;
import cube.service.multipointcomm.MultipointCommService;
import cube.service.multipointcomm.SignalingCallback;
import cube.service.multipointcomm.signaling.AnswerSignaling;
import cube.service.multipointcomm.signaling.Signaling;

/**
 * Answer 信令任务。
 */
public class AnswerTask extends ServiceTask {

    public AnswerTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        AnswerSignaling answer = new AnswerSignaling(packet.data);

        MultipointCommService service = (MultipointCommService) this.kernel.getModule(MultipointCommService.NAME);

        // 处理 Answer
        service.processAnswer(answer, new SignalingCallback() {
            @Override
            public void on(MultipointCommStateCode stateCode, Signaling signaling) {
                cellet.speak(talkContext,
                        makeResponse(action, packet, MultipointCommAction.AnswerAck.name, stateCode.code, signaling.toJSON()));
                markResponseTime();
            }
        });
    }
}
