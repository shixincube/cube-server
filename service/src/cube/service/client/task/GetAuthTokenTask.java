/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
