/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.container;

import cube.app.server.account.Account;
import cube.app.server.account.AccountManager;
import cube.app.server.account.StateCode;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 搜索账号。
 */
public class SearchAccountHandler extends ContextHandler {

    public SearchAccountHandler(String httpOrigin, String httpsOrigin) {
        super("/account/search/");
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
            if (null == data) {
                this.respond(response, HttpStatus.NOT_ACCEPTABLE_406);
                return;
            }

            Account account = null;
            if (data.containsKey("token") && data.containsKey("id")) {
                account = AccountManager.getInstance().searchAccountWithId(Long.parseLong(data.get("id")));
            }
            else if (data.containsKey("token") && data.containsKey("phone")) {
                account = AccountManager.getInstance().searchAccountWithPhoneNumber(data.get("phone"));
            }
            else {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                return;
            }

            JSONObject responseData = new JSONObject();
            if (null != account) {
                responseData.put("code", StateCode.Success.code);
                responseData.put("account", account.toCompactJSON());
            }
            else {
                responseData.put("code", StateCode.NotFindAccount.code);
            }
            this.respondOk(response, responseData);
        }
    }
}
