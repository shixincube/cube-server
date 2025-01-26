/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.container.handler;

import cube.console.container.Handlers;
import cube.console.mgmt.UserManager;
import cube.console.mgmt.UserToken;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 用户签出。
 */
public class SignOutHandler extends ContextHandler {

    private final static String COOKIE_NAME_TOKEN = "CubeConsoleToken";

    private UserManager userManager;

    public SignOutHandler(UserManager userManager) {
        super("/signout");
        setHandler(new Handler());
        this.userManager = userManager;
    }

    protected class Handler extends AbstractHandler {

        public Handler() {
            super();
        }

        @Override
        public void handle(String target, Request baseRequest,
                           HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            if (baseRequest.getMethod().equalsIgnoreCase(HttpMethod.GET.asString())) {
                baseRequest.setHandled(true);
                return;
            }

            String tokenString = Handlers.getToken(request);
            if (null == tokenString) {
                response.setStatus(HttpStatus.UNAUTHORIZED_401);
                baseRequest.setHandled(true);
                return;
            }

            // 签出
            userManager.signOut(tokenString);

            Cookie cookie = new Cookie(COOKIE_NAME_TOKEN, tokenString);
            cookie.setMaxAge(1);
            cookie.setPath("/");
            response.addCookie(cookie);

            response.setStatus(HttpStatus.FOUND_302);
            response.setHeader("Location", "/index.html");
            baseRequest.setHandled(true);
        }
    }
}
