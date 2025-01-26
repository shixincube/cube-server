/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.container;

import cube.app.server.account.Account;
import cube.app.server.account.AccountManager;
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
 * Cube 配置信息。
 */
public class CubeConfigHandler extends ContextHandler {

    private JSONObject cubeConfig;

    public CubeConfigHandler(String httpOrigin, String httpsOrigin, JSONObject cubeConfig) {
        super("/cube/config/");
        this.cubeConfig = cubeConfig;
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
            // 判断 Token 是否有效
            Map<String, String> data = this.parseQueryStringParams(request);
            if (null == data) {
                this.respond(response, HttpStatus.NOT_ACCEPTABLE_406);
                return;
            }

            String token = data.containsKey("t") ? data.get("t") : data.get("token");

            if (null == token) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                return;
            }

            if (AccountManager.getInstance().isValidToken(token)) {
                this.respondOk(response, cubeConfig);
            }
            else {
                this.respond(response, HttpStatus.FORBIDDEN_403);
            }
        }
    }
}
