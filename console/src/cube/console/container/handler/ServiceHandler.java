/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.container.handler;

import cell.util.log.Logger;
import cube.console.Console;
import cube.console.container.Handlers;
import cube.console.mgmt.ServiceServer;
import cube.console.mgmt.UserToken;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.Charset;

/**
 * 模块服务单元相关操作。
 */
public class ServiceHandler extends ContextHandler {

    private Console console;

    public ServiceHandler(Console console) {
        super("/service");
        this.setHandler(new Handler());
        this.console = console;
    }

    /**
     * 处理句柄。
     */
    protected class Handler extends CrossDomainHandler {

        protected Handler() {
            super();
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            if (!Handlers.checkCookie(request, console)) {
                respond(response, HttpStatus.UNAUTHORIZED_401);
                complete();
                return;
            }

            if (target.equals("/status")) {
                String tag = request.getParameter("tag");
                String deployPath = request.getParameter("path");

                ServiceServer server = console.getServiceManager().getServiceServer(tag, deployPath);
                if (null == server) {
                    respond(response, HttpStatus.NOT_FOUND_404);
                    complete();
                    return;
                }

                respondOk(response, server.toJSON());
            }
            else if (target.equals("/start")) {
                // 启动服务器
                String password = request.getParameter("pwd");
                String tag = request.getParameter("tag");
                String deployPath = request.getParameter("path");

                if (null == password || null == tag || null == deployPath) {
                    respond(response, HttpStatus.FORBIDDEN_403);
                    complete();
                    return;
                }

                String tokenString = Handlers.getToken(request);

                // 校验密码
                UserToken token = console.getUserManager().getToken(tokenString);
                if (!token.user.validatePassword(password)) {
                    respond(response, HttpStatus.UNAUTHORIZED_401);
                    complete();
                    return;
                }

                // 启动
                ServiceServer server = console.getServiceManager().startService(tag, deployPath, password);
                respondOk(response, server.toJSON());
            }
            else if (target.equals("/stop")) {
                // 关闭服务器
                String password = request.getParameter("pwd");
                String tag = request.getParameter("tag");
                String deployPath = request.getParameter("path");

                if (null == password || null == tag || null == deployPath) {
                    respond(response, HttpStatus.FORBIDDEN_403);
                    complete();
                    return;
                }

                String tokenString = Handlers.getToken(request);

                // 校验密码
                UserToken token = console.getUserManager().getToken(tokenString);
                if (!token.user.validatePassword(password)) {
                    respond(response, HttpStatus.UNAUTHORIZED_401);
                    complete();
                    return;
                }

                // 停止
                ServiceServer server = console.getServiceManager().stopService(tag, deployPath, password);
                respondOk(response, server.toJSON());
            }
            else if (target.equals("/config")) {
                // 更新配置
                String configString = null;
                InputStream is = request.getInputStream();
                int length = 0;
                byte[] bytes = new byte[30 * 1024];
                while ((length = is.read(bytes)) > 0) {
                    configString = new String(bytes, 0, length, Charset.forName("UTF-8"));
                }

                if (null == configString) {
                    respond(response, HttpStatus.FORBIDDEN_403);
                    complete();
                    return;
                }

                int index = configString.indexOf("=");
                configString = URLDecoder.decode(configString.substring(index + 1), "UTF-8");

                JSONObject serverJson = new JSONObject(configString);

                ServiceServer server = null;
                try {
                    server = console.getServiceManager().updateServiceServer(serverJson);
                } catch (Exception e) {
                    Logger.w(DispatcherHandler.class, "/config", e);
                }

                if (null == server) {
                    respond(response, HttpStatus.BAD_REQUEST_400);
                    complete();
                    return;
                }

                respondOk(response, server.toJSON());
            }

            complete();
        }
    }
}
