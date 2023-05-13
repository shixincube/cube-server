/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.service.aigc;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.common.action.AIGCAction;
import cube.core.AbstractCellet;
import cube.core.Kernel;
import cube.service.aigc.task.*;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * AIGC 服务单元。
 */
public class AIGCCellet extends AbstractCellet {

    private AIGCService service;

    private ConcurrentLinkedQueue<Responder> responderList;

    public AIGCCellet() {
        super(AIGCService.NAME);
        this.responderList = new ConcurrentLinkedQueue<>();
    }

    @Override
    public boolean install() {
        this.service = new AIGCService(this);

        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.installModule(AIGCService.NAME, this.service);

        return true;
    }

    @Override
    public void uninstall() {
        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.uninstallModule(AIGCService.NAME);

        for (Responder responder : this.responderList) {
            responder.finish();
        }
        this.responderList.clear();
    }

    public AIGCService getService() {
        return this.service;
    }

    public ActionDialect transmit(TalkContext talkContext, ActionDialect dialect) {
        Responder responder = new Responder(dialect);
        this.responderList.add(responder);

        if (!this.speak(talkContext, dialect)) {
            Logger.w(AIGCCellet.class, "Speak session error: " + talkContext.getSessionHost());
            this.responderList.remove(responder);
            return null;
        }

        ActionDialect response = responder.waitingFor(3 * 60 * 1000);
        if (null == response) {
            Logger.w(AIGCCellet.class, "Response is null: " + talkContext.getSessionHost());
            this.responderList.remove(responder);
            return null;
        }

        return response;
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        ActionDialect dialect = new ActionDialect(primitive);
        String action = dialect.getName();

        if (dialect.containsParam(Responder.NotifierKey)) {
            // 应答阻塞访问
            for (Responder responder : this.responderList) {
                if (responder.isResponse(dialect)) {
                    responder.notifyResponse(dialect);
                    this.responderList.remove(responder);
                    break;
                }
            }
        }
        else if (AIGCAction.CheckToken.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new CheckTokenTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.NaturalLanguageTask.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new NLGeneralTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.Sentiment.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new SentimentTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.Chat.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new ChatTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.Conversation.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new ConversationTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.SearchCommand.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new SearchCommandTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.RequestChannel.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new RequestChannelTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.AutomaticSpeechRecognition.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new AutomaticSpeechRecognitionTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.Setup.name.equals(action)) {
            // 来自 Unit 的请求
            this.execute(new SetupTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.Teardown.name.equals(action)) {
            // 来自 Unit 的请求
            this.execute(new TeardownTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
    }
}
