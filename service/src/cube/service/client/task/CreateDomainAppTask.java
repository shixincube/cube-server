/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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
