/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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
import cube.common.entity.ContactBehavior;
import cube.common.notice.NoticeData;
import cube.common.state.RiskMgmtStateCode;
import cube.core.AbstractModule;
import cube.service.client.ClientCellet;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 批量获取联系人行为。
 */
public class ListContactBehaviorsTask extends ClientTask {

    public ListContactBehaviorsTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        ActionDialect response = new ActionDialect(ClientAction.ListContactBehaviors.name);
        copyNotifier(response);

        AbstractModule module = this.getRiskMgmtService();
        if (null == module) {
            response.addParam(NoticeData.CODE, RiskMgmtStateCode.Failure.code);
            this.cellet.speak(this.talkContext, response);
            return;
        }

        JSONObject notification = this.actionDialect.getParamAsJson(NoticeData.PARAMETER);
        List<ContactBehavior> result = module.notify(notification);
        if (null == result) {
            response.addParam(NoticeData.CODE, RiskMgmtStateCode.InvalidParameter.code);
            this.cellet.speak(this.talkContext, response);
            return;
        }

        response.addParam(NoticeData.CODE, RiskMgmtStateCode.Ok.code);

        JSONArray array = new JSONArray();
        for (ContactBehavior behavior : result) {
            array.put(behavior.toCompactJSON());
        }
        notification.put("list", array);
        response.addParam(NoticeData.DATA, notification);

        this.cellet.speak(this.talkContext, response);
    }
}