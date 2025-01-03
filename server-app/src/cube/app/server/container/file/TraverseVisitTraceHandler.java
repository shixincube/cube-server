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

package cube.app.server.container.file;

import cube.app.server.Manager;
import cube.app.server.account.Account;
import cube.app.server.account.AccountManager;
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
 * 遍历访问记录层级。
 */
public class TraverseVisitTraceHandler extends ContextHandler {

    public TraverseVisitTraceHandler(String httpOrigin, String httpsOrigin) {
        super("/file/traverse/trace/");
        this.setHandler(new Handler(httpOrigin, httpsOrigin));
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
            Account account = AccountManager.getInstance().getOnlineAccount(tokenCode);
            if (null == account) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            String code = data.get("code");
            List<VisitTrace> result = Manager.getInstance().getClient().getFileProcessor()
                    .traverseVisitTrace(account.id, account.domain, code);
            JSONArray array = new JSONArray();
            for (VisitTrace visitTrace : result) {
                array.put(visitTrace.toCompactJSON());
            }

            JSONObject responseData = new JSONObject();
            responseData.put("sharingCode", code);
            responseData.put("list", array);
            this.respondOk(response, responseData);
            this.complete();
        }
    }
}
