/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.container;

import cube.app.server.account.AccountManager;
import cube.app.server.version.AppVersion;
import cube.app.server.version.VersionManager;
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
 * 通知处理。
 */
public class VersionHandler extends ContextHandler {

    public VersionHandler(String httpOrigin, String httpsOrigin) {
        super("/version/");
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
            if (null == data || !data.containsKey("token") || !data.containsKey("device")) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                return;
            }

            String token = data.get("token");
            if (!AccountManager.getInstance().isValidToken(token)) {
                // 无效令牌
                this.respond(response, HttpStatus.FORBIDDEN_403);
                return;
            }

            String device = data.get("device");

            AppVersion version = VersionManager.getInstance().getVersion(device);
            if (null == version) {
                version = new AppVersion(device, 3, 0, 0, false);
            }

            JSONObject responseData = new JSONObject();
            responseData.put("device", device);
            responseData.put("version", version.toJSON());

            this.respondOk(response, responseData);
        }
    }
}
