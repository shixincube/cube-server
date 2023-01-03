/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.console.container.handler;

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
 * 用户签入。
 */
public class SignInHandler extends ContextHandler {

    private final static String COOKIE_NAME_TOKEN = "CubeConsoleToken";

    private UserManager userManager;

    public SignInHandler(UserManager userManager) {
        super("/signin");
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

            String username = request.getParameter("username");
            String password = request.getParameter("password");
            if (null == username || null == password) {

                // 尝试读取 Cookie
                Cookie[] cookies = request.getCookies();
                if (null != cookies) {
                    for (Cookie cookie : cookies) {
                        if (COOKIE_NAME_TOKEN.equalsIgnoreCase(cookie.getName())) {
                            // 发现当前请求包含 Cookie 信息
                            String value = cookie.getValue();
                            UserToken token = userManager.signIn(value);
                            if (null != token) {
                                response.setStatus(HttpStatus.OK_200);
                                response.setContentType("application/json");
                                response.getWriter().write(token.toJSON().toString());
                            }
                            else {
                                response.setStatus(HttpStatus.BAD_REQUEST_400);
                            }
                            baseRequest.setHandled(true);
                            return;
                        }
                    }
                }

                response.setStatus(HttpStatus.FOUND_302);
                response.setHeader("Location", "/index.html?e=" + 9);
                baseRequest.setHandled(true);
                return;
            }

            UserToken token = userManager.signIn(username, password);
            if (null == token) {
                response.setStatus(HttpStatus.FOUND_302);
                response.setHeader("Location", "/index.html?e=" + 10);
                baseRequest.setHandled(true);
                return;
            }

            Cookie cookie = new Cookie(COOKIE_NAME_TOKEN, token.token);
            cookie.setMaxAge(token.getAgeInSeconds());
            cookie.setPath("/");
            response.addCookie(cookie);

            response.setStatus(HttpStatus.FOUND_302);
            response.setHeader("Location", "/dashboard.html");
            baseRequest.setHandled(true);
        }
    }
}
