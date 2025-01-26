/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
