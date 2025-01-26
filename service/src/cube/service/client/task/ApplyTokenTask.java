/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.client.task;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.auth.AuthToken;
import cube.common.action.ClientAction;
import cube.service.client.ClientCellet;

/**
 * 申请令牌任务。
 */
public class ApplyTokenTask extends ClientTask {

    public ApplyTokenTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        String domain = actionDialect.getParamAsString("domain");
        String appKey = actionDialect.getParamAsString("appKey");
        Long cid = actionDialect.getParamAsLong("cid");
        long duration = 0;

        if (actionDialect.containsParam("duration")) {
            duration = actionDialect.getParamAsLong("duration");
        }

        AuthToken token = null;

        if (duration > 0) {
            token = getAuthService().applyToken(domain, appKey, cid, duration);
        }
        else {
            token = getAuthService().applyToken(domain, appKey, cid);
        }

        ActionDialect result = new ActionDialect(ClientAction.ApplyToken.name);
        copyNotifier(result);
        result.addParam("token", token.toJSON());

        cellet.speak(talkContext, result);
    }
}
