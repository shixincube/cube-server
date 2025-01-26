/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.container;

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

/**
 * 账号登出。
 */
public class LogoutHandler extends ContextHandler {

    public LogoutHandler(String httpOrigin, String httpsOrigin) {
        super("/account/logout/");
        setHandler(new Handler(httpOrigin, httpsOrigin));
    }

    protected class Handler extends CrossDomainHandler {

        public Handler(String httpOrigin, String httpsOrigin) {
            super();
            setHttpAllowOrigin(httpOrigin);
            setHttpsAllowOrigin(httpsOrigin);
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            // 读取数据
            JSONObject data = this.readBodyAsJSONObject(request);
            String token = data.getString("token");
            String device = data.getString("device");

            if (null == token || null == device) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                return;
            }

            StateCode stateCode = AccountManager.getInstance().logout(token, device);

            JSONObject responseData = new JSONObject();
            responseData.put("code", stateCode.code);
            responseData.put("token", token);

            this.respondOk(response, responseData);
        }
    }
}
