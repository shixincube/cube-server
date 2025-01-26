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
import cube.common.entity.IceServer;
import cube.service.client.ClientCellet;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建域应用任务。
 */
public class CreateDomainAppTask extends ClientTask {

    public CreateDomainAppTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        String domainName = actionDialect.getParamAsString("domainName");
        String appKey = actionDialect.getParamAsString("appKey");
        String appId = actionDialect.getParamAsString("appId");
        Endpoint mainEndpoint = new Endpoint(actionDialect.getParamAsJson("mainEndpoint"));
        Endpoint httpEndpoint = new Endpoint(actionDialect.getParamAsJson("httpEndpoint"));
        Endpoint httpsEndpoint = new Endpoint(actionDialect.getParamAsJson("httpsEndpoint"));

        List<IceServer> iceServers = null;
        if (actionDialect.containsParam("iceServers")) {
            iceServers = new ArrayList<>();
            JSONObject data = actionDialect.getParamAsJson("iceServers");
            JSONArray array = data.getJSONArray("list");
            for (int i = 0; i < array.length(); ++i) {
                JSONObject iceJson = array.getJSONObject(i);
                IceServer iceServer = new IceServer(iceJson);
                iceServers.add(iceServer);
            }
        }

        boolean ferry = actionDialect.getParamAsBool("ferry");

        // 创建域应用
        AuthDomain authDomain = getAuthService().createDomainApp(domainName, appKey, appId,
                 mainEndpoint, httpEndpoint, httpsEndpoint, iceServers, ferry);

        try {
            if (ferry) {
                IceServer iceServer = new IceServer(authDomain.iceServers.getJSONObject(0));

                // 创建访问点
                JSONObject ferryActionData = new JSONObject();
                ferryActionData.put("action", "createAccessPoint");
                ferryActionData.put("domain", domainName);
                ferryActionData.put("main", actionDialect.getParamAsJson("mainEndpoint"));
                ferryActionData.put("http", actionDialect.getParamAsJson("httpEndpoint"));
                ferryActionData.put("https", actionDialect.getParamAsJson("httpsEndpoint"));
                ferryActionData.put("iceServer", iceServer.toJSON());
                getFerryService().notify(ferryActionData);
            }
        } catch (Exception e) {
            Logger.w(this.getClass(), "Process ferry account point error", e);
        }

        ActionDialect result = new ActionDialect(ClientAction.CreateDomainApp.name);
        copyNotifier(result);
        result.addParam("authDomain", authDomain.toJSON());

        cellet.speak(talkContext, result);
    }
}
