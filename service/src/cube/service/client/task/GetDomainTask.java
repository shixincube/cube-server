/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.client.task;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.common.action.ClientAction;
import cube.common.entity.AuthDomain;
import cube.service.client.ClientCellet;

/**
 * 获取域数据任务。
 */
public class GetDomainTask extends ClientTask {

    public GetDomainTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        String domain = actionDialect.getParamAsString("domain");
        String appKey = actionDialect.containsParam("appKey") ?
                actionDialect.getParamAsString("appKey") : null;

        ActionDialect result = new ActionDialect(ClientAction.GetDomain.name);
        copyNotifier(result);

        AuthDomain authDomain = getAuthService().getAuthDomain(domain, appKey);
        if (null != authDomain) {
            result.addParam("authDomain", authDomain.toJSON());
        }

        cellet.speak(talkContext, result);
    }
}
