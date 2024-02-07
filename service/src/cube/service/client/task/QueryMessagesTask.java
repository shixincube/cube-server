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
import cube.common.action.ClientAction;
import cube.common.action.MessagingAction;
import cube.core.AbstractModule;
import cube.service.client.ClientCellet;
import org.json.JSONObject;

/**
 * 查询消息任务。
 */
public class QueryMessagesTask extends ClientTask {

    /**
     * 构造函数。
     *
     * @param cellet
     * @param talkContext
     * @param actionDialect
     */
    public QueryMessagesTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        ActionDialect response = new ActionDialect(ClientAction.PushMessage.name);
        copyNotifier(response);

        AbstractModule module = this.getMessagingModule();
        if (null == module) {
            cellet.speak(talkContext, response);
            return;
        }

        long beginning = actionDialect.getParamAsLong("beginning");
        long ending = actionDialect.getParamAsLong("ending");
        String domain = actionDialect.getParamAsString("domain");
        Long contactId = actionDialect.containsParam("contactId")
                ? actionDialect.getParamAsLong("contactId") : null;
        Long groupId = actionDialect.containsParam("groupId")
                ? actionDialect.getParamAsLong("groupId") : null;

        JSONObject notification = new JSONObject();
        notification.put("action", MessagingAction.Pull.name);
        notification.put("beginning", beginning);
        notification.put("ending", ending);

        notification.put("domain", domain);

        if (null != contactId) {
            notification.put("contactId", contactId.longValue());
        }
        else if (null != groupId) {
            notification.put("groupId", groupId.longValue());
        }

        // 使用 notify 通知模块
        JSONObject result = (JSONObject) module.notify(notification);
        if (null != result) {
            response.addParam("result", result);
        }

        cellet.speak(talkContext, response);
    }
}
