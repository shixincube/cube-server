/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
