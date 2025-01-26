/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.container;

import cube.console.Console;
import cube.console.container.handler.ReportHandler;
import cube.console.container.handler.*;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.File;

/**
 * HTTP 处理句柄集合。
 */
public final class Handlers {

    private final static String COOKIE_NAME_TOKEN = "CubeConsoleToken";

    private Handlers() {
    }

    /**
     * 创建所有的 Handler 实例。
     *
     * @param server
     * @param console
     * @return
     */
    public static HandlerList createHandlerList(Server server, Console console) {
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setWelcomeFiles(new String[] { "index.html" });

        // 判断目录
        File path = new File("web");
        if (path.exists() && path.isDirectory()) {
            resourceHandler.setResourceBase("web");
        }
        else {
            resourceHandler.setResourceBase("WebContent");
        }

        ContextHandler indexHandler = new ContextHandler("/");
        indexHandler.setHandler(resourceHandler);

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] {
                // 索引页
                indexHandler,

                // For RESTful API
                new ReportHandler(console),

                // For AJAX API
                new SignInHandler(console.getUserManager()),
                new SignOutHandler(console.getUserManager()),
                new ServersHandler(console),
                new DeployHandler(console),
                new DispatcherHandler(console),
                new ServiceHandler(console),
                new AuthHandler(console),
                new StatisticDataHandler(console),

                new ServerLogHandler(console),
                new ServerReportHandler(console),

                new StopHandler(server, console),
                new DefaultHandler()});
        return handlers;
    }

    /**
     * 校验 Cookie 。
     *
     * @param request
     * @param console
     * @return
     */
    public static boolean checkCookie(HttpServletRequest request, Console console) {
        // 尝试读取 Cookie
        Cookie[] cookies = request.getCookies();
        if (null != cookies) {
            for (Cookie cookie : cookies) {
                if (COOKIE_NAME_TOKEN.equalsIgnoreCase(cookie.getName())) {
                    // 发现当前请求包含 Cookie 信息
                    String value = cookie.getValue();
                    return console.getUserManager().checkToken(value);
                }
            }
        }

        return false;
    }

    /**
     * 获取访问的 Token
     *
     * @param request
     * @return
     */
    public static String getToken(HttpServletRequest request) {
        // 尝试读取 Cookie
        Cookie[] cookies = request.getCookies();
        if (null != cookies) {
            for (Cookie cookie : cookies) {
                if (COOKIE_NAME_TOKEN.equalsIgnoreCase(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
