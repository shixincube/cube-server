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

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 管理账号。
 */
public class ManagementHandler extends ContextHandler {

    public ManagementHandler(String httpOrigin, String httpsOrigin) {
        super("/account/management/");
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
            if (request.getRequestURI().indexOf("favicon") > 0) {
                byte[] data = Files.readAllBytes(Paths.get("assets/favicon.png"));
                response.setContentType("image/png");
                response.getOutputStream().write(data);
                response.getOutputStream().close();
                response.setStatus(HttpStatus.OK_200);
                this.complete();
                return;
            }

            String url = AccountManager.getInstance().getUpgradeExternalURL();
            if (null == url) {
                byte[] data = Files.readAllBytes(Paths.get("assets/management.html"));
                response.setContentType("text/html");
                response.setCharacterEncoding("UTF-8");
                response.getOutputStream().write(data);
                response.getOutputStream().close();
                response.setStatus(HttpStatus.OK_200);
                this.complete();
                return;
            }

            String token = null;
            Cookie[] cookies = request.getCookies();
            if (null != cookies) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equalsIgnoreCase("CubeAppToken")) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }

            if (null != token) {
                if (url.indexOf("?") > 0) {
                    url += "&_t=" + token;
                }
                else {
                    url += "?_t=" + token;
                }
            }

            response.setStatus(HttpStatus.MOVED_PERMANENTLY_301);
            response.setHeader("Location", url);
            this.complete();
        }
    }
}
