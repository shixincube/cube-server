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
import cube.auth.AuthToken;
import cube.auth.PrimaryDescription;
import cube.common.action.ClientAction;
import cube.common.state.AuthStateCode;
import cube.service.auth.AuthService;
import cube.service.client.ClientCellet;
import org.json.JSONObject;

/**
 * 注入访问令牌任务。
 */
public class InjectAuthTokenTask extends ClientTask {

    public InjectAuthTokenTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        ActionDialect response = new ActionDialect(ClientAction.InjectAuthToken.name);
        copyNotifier(response);

        JSONObject tokenJson = this.actionDialect.getParamAsJson("token");
        AuthToken authToken = new AuthToken(tokenJson);

        AuthService authService = this.getAuthService();

        PrimaryDescription primaryDescription = authService.getPrimaryDescription(authToken.getDomain(), authToken.getAppKey());
        if (null == primaryDescription) {
            response.addParam("code", AuthStateCode.InvalidParameter.code);
            cellet.speak(talkContext, response);
            return;
        }

        // 设置描述
        authToken.setDescription(primaryDescription);

        // 注入指定令牌
        authService.injectToken(authToken);

        response.addParam("code", AuthStateCode.Ok.code);
        response.addParam("token", authToken.toJSON());
        cellet.speak(talkContext, response);
    }
}
