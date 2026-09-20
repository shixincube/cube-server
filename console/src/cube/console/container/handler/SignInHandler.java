/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.container.handler;

import cube.console.mgmt.UserManager;
import cube.console.mgmt.UserToken;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 用户签入。
 *
 * 支持两种调用方式：
 * <ul>
 *     <li>XHR（请求头 {@code X-Requested-With: XMLHttpRequest} 或 {@code Accept: application/json}）：
 *     直接返回 {@link UserToken} 的 JSON 数据，由单页应用自行处理跳转。</li>
 *     <li>传统表单提交：保持原有的 302 跳转语义。</li>
 * </ul>
 */
public class SignInHandler extends ContextHandler {

    private final static String COOKIE_NAME_TOKEN = "CubeConsoleToken";

    /** 登录页，携带错误码 {@code e} 回跳 */
    private final static String LOGIN_PAGE = "/login";

    private UserManager userManager;

    public SignInHandler(UserManager userManager) {
        super("/signin");
        // 接口挂在上下文根路径上：允许直接访问 /signin ，避免 Jetty 将其 302 重定向到 /signin/
        // （重定向会让 POST 被降级为 GET，导致登录静默失败）
        setAllowNullPathInfo(true);
        setHandler(new Handler());
        this.userManager = userManager;
    }

    /**
     * 判断是否为前端 XHR 调用。
     *
     * @param request HTTP 请求
     * @return 需要返回 JSON 时返回 {@code true}
     */
    private static boolean isJsonRequest(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        if (null != requestedWith && requestedWith.equalsIgnoreCase("XMLHttpRequest")) {
            return true;
        }

        String accept = request.getHeader("Accept");
        return null != accept && accept.contains("application/json");
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

            boolean json = isJsonRequest(request);

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
                                if (json) {
                                    respondToken(response, token);
                                }
                                else {
                                    redirect(response, "/");
                                }
                            }
                            else {
                                respondFailure(response, json,
                                        HttpStatus.UNAUTHORIZED_401, "登录状态已失效");
                            }
                            baseRequest.setHandled(true);
                            return;
                        }
                    }
                }

                respondFailure(response, json, HttpStatus.UNAUTHORIZED_401, "缺少账号或口令");
                baseRequest.setHandled(true);
                return;
            }

            UserToken token = userManager.signIn(username, password);
            if (null == token) {
                respondFailure(response, json, HttpStatus.UNAUTHORIZED_401, "账号或口令不正确");
                baseRequest.setHandled(true);
                return;
            }

            Cookie cookie = new Cookie(COOKIE_NAME_TOKEN, token.token);
            cookie.setMaxAge(token.getAgeInSeconds());
            cookie.setPath("/");
            response.addCookie(cookie);

            if (json) {
                respondToken(response, token);
            }
            else {
                redirect(response, "/");
            }

            baseRequest.setHandled(true);
        }

        /**
         * 返回令牌数据。
         */
        private void respondToken(HttpServletResponse response, UserToken token) throws IOException {
            response.setStatus(HttpStatus.OK_200);
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().write(token.toJSON().toString());
        }

        /**
         * 返回登录失败。
         */
        private void respondFailure(HttpServletResponse response, boolean json, int status, String message)
                throws IOException {
            if (json) {
                JSONObject data = new JSONObject();
                data.put("error", message);
                response.setStatus(status);
                response.setContentType("application/json; charset=UTF-8");
                response.getWriter().write(data.toString());
            }
            else {
                // 兼容旧的表单提交：回到登录页并携带错误码
                redirect(response, LOGIN_PAGE + "?e=" + (HttpStatus.UNAUTHORIZED_401 == status ? 10 : 9));
            }
        }

        /**
         * 发送 302 跳转。
         */
        private void redirect(HttpServletResponse response, String location) {
            response.setStatus(HttpStatus.FOUND_302);
            response.setHeader("Location", location);
        }
    }
}
