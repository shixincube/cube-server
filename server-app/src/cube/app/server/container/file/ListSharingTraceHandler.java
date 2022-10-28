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

package cube.app.server.container.file;

import cube.app.server.Manager;
import cube.app.server.account.Account;
import cube.app.server.account.AccountManager;
import cube.app.server.util.DefenseStrategy;
import cube.client.Client;
import cube.common.entity.VisitTrace;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 批量获取分享痕迹数据。
 */
public class ListSharingTraceHandler extends ContextHandler {

    private DefenseStrategy strategy;

    public ListSharingTraceHandler(String httpOrigin, String httpsOrigin) {
        super("/file/list/trace/");
        // 冷却时间 1000ms
        this.strategy = new DefenseStrategy(1000);
        setHandler(new Handler(httpOrigin, httpsOrigin));
    }

    protected class Handler extends CrossDomainHandler {

        public Handler(String httpOrigin, String httpsOrigin) {
            super();
            setHttpAllowOrigin(httpOrigin);
            setHttpsAllowOrigin(httpsOrigin);
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            Map<String, String> data = this.parseQueryStringParams(request);
            if (null == data || !data.containsKey("token") || !data.containsKey("code")) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
                return;
            }

            String tokenCode = data.get("token");

            if (!strategy.verify(tokenCode)) {
                this.respond(response, HttpStatus.NOT_ACCEPTABLE_406);
                this.complete();
                return;
            }

            int begin = Integer.parseInt(data.get("begin"));
            int end = Integer.parseInt(data.get("end"));
            String code = data.get("code");

            Account account = AccountManager.getInstance().getOnlineAccount(tokenCode);
            if (null == account) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            JSONObject responseData = new JSONObject();
            responseData.put("begin", begin);
            responseData.put("end", end);
            responseData.put("sharingCode", code);

            JSONArray array = new JSONArray();

            Client client = Manager.getInstance().getClient();
            List<VisitTrace> list = client.getFileStorage().listVisitTraces(account.id, account.domain, code, begin, end);
            if (null != list) {
                for (VisitTrace trace : list) {
                    array.put(trace.toCompactJSON());
                }
            }

            responseData.put("list", array);

            // 总数
            responseData.put("total", client.getFileStorage().getVisitTraceTotal(code));

            this.respondOk(response, responseData);
            this.complete();
        }
    }
}
