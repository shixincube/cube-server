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

package cube.service.client.task;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.core.AbstractModule;
import cube.service.aigc.AIGCService;
import cube.service.auth.AuthService;
import cube.service.client.ClientCellet;
import org.json.JSONObject;

/**
 * 服务器客户端执行任务。
 */
public abstract class ClientTask implements Runnable {

    protected final ClientCellet cellet;

    protected final TalkContext talkContext;

    protected final ActionDialect actionDialect;

    /**
     * 构造函数。
     *
     * @param cellet
     * @param talkContext
     * @param actionDialect
     */
    public ClientTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        this.cellet = cellet;
        this.talkContext = talkContext;
        this.actionDialect = actionDialect;
    }

    protected JSONObject extractNotifier() {
        if (actionDialect.containsParam("_notifier")) {
            return actionDialect.getParamAsJson("_notifier");
        }

        return null;
    }

    protected JSONObject copyNotifier(ActionDialect destination) {
        if (actionDialect.containsParam("_notifier")) {
            destination.addParam("_notifier", actionDialect.getParamAsJson("_notifier"));
            return actionDialect.getParamAsJson("_notifier");
        }
        else if (actionDialect.containsParam("_performer")) {
            destination.addParam("_performer", actionDialect.getParamAsJson("_performer"));
            return actionDialect.getParamAsJson("_performer");
        }
        else {
            return null;
        }
    }

    protected AuthService getAuthService() {
        return (AuthService) cellet.getKernel().getModule(AuthService.NAME);
    }

    protected AbstractModule getMessagingModule() {
        return cellet.getKernel().getModule("Messaging");
    }

    protected AbstractModule getFileStorageModule() {
        return cellet.getKernel().getModule("FileStorage");
    }

    protected AbstractModule getFileProcessorModule() {
        return cellet.getKernel().getModule("FileProcessor");
    }

    protected AbstractModule getFerryService() {
        return cellet.getKernel().getModule("Ferry");
    }

    protected AbstractModule getRiskMgmtService() {
        return cellet.getKernel().getModule("RiskMgmt");
    }

    protected AIGCService getAIGCService() {
        return (AIGCService) cellet.getKernel().getModule("AIGC");
    }
}
