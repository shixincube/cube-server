/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.container;

import cube.app.server.account.AccountManager;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 心跳。
 */
public class HeartbeatHandler extends ContextHandler {

    public HeartbeatHandler(String httpOrigin, String httpsOrigin) {
        super("/account/hb/");
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
            JSONObject data = this.readBodyAsJSONObject(request);
            if (!data.has("token")) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                return;
            }

            String token = data.getString("token");
            boolean result = AccountManager.getInstance().heartbeat(token);

            JSONObject responseData = new JSONObject();
            responseData.put("success", result);

            this.respondOk(response, responseData);
        }
    }
}
