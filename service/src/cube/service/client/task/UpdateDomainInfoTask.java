/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.client.task;

import cell.core.net.Endpoint;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.common.action.ClientAction;
import cube.common.entity.AuthDomain;
import cube.service.client.ClientCellet;
import org.json.JSONObject;

/**
 * 创建域应用任务。
 */
public class UpdateDomainInfoTask extends ClientTask {

    public UpdateDomainInfoTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        String domainName = actionDialect.getParamAsString("domainName");
        Endpoint mainEndpoint = new Endpoint(actionDialect.getParamAsJson("mainEndpoint"));
        Endpoint httpEndpoint = new Endpoint(actionDialect.getParamAsJson("httpEndpoint"));
        Endpoint httpsEndpoint = new Endpoint(actionDialect.getParamAsJson("httpsEndpoint"));

        // 更新域信息
        AuthDomain authDomain = getAuthService().updateDomain(domainName, mainEndpoint, httpEndpoint, httpsEndpoint);

        try {
            if (null != authDomain) {
                // 创建访问点
                JSONObject ferryActionData = new JSONObject();
                ferryActionData.put("action", "updateAccessPoint");
                ferryActionData.put("domain", domainName);
                ferryActionData.put("main", actionDialect.getParamAsJson("mainEndpoint"));
                ferryActionData.put("http", actionDialect.getParamAsJson("httpEndpoint"));
                ferryActionData.put("https", actionDialect.getParamAsJson("httpsEndpoint"));
                getFerryService().notify(ferryActionData);
            }
        } catch (Exception e) {
            Logger.w(this.getClass(), "Process ferry account point error", e);
        }

        ActionDialect result = new ActionDialect(ClientAction.UpdateDomain.name);
        copyNotifier(result);
        if (null != authDomain) {
            result.addParam("authDomain", authDomain.toJSON());
        }

        cellet.speak(talkContext, result);
    }
}
