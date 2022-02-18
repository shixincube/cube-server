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
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.common.action.ClientAction;
import cube.common.state.AuthStateCode;
import cube.service.auth.AuthService;
import cube.service.client.ClientCellet;

/**
 * 获取访问令牌信息。
 */
public class GetAuthTokenTask extends ClientTask {

    public GetAuthTokenTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        String tokenCode = null;
        Long contactId = null;

        if (actionDialect.containsParam("tokenCode")) {
            tokenCode = actionDialect.getParamAsString("tokenCode");
        }
        else if (actionDialect.containsParam("contactId")) {
            contactId = actionDialect.getParamAsLong("contactId");
        }

        ActionDialect response = new ActionDialect(ClientAction.GetAuthToken.name);
        copyNotifier(response);

        // 获取访问令牌
        AuthService module = this.getAuthService();
        AuthToken authToken = (null != tokenCode) ? module.getToken(tokenCode) :
                module.queryAuthTokenByContactId(contactId);
        if (null == authToken) {
            response.addParam("code", AuthStateCode.Failure.code);
            cellet.speak(talkContext, response);
            return;
        }

        Logger.d(this.getClass(), "Gets auth token : " + tokenCode);

        response.addParam("code", AuthStateCode.Ok.code);
        response.addParam("token", authToken.toJSON());
        cellet.speak(talkContext, response);
    }
}
