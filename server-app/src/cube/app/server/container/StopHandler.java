/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.container;

import cell.util.log.Logger;
import cube.util.HttpServer;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 停止服务器处理句柄。
 */
public class StopHandler extends ContextHandler {

    private HttpServer server;

    private ContainerManager manager;

    public StopHandler(HttpServer server, ContainerManager manager) {
        super("/stop");
        this.setHandler(new Handler());
        this.server = server;
        this.manager = manager;
    }

    protected class Handler extends AbstractHandler {

        @Override
        public void handle(String target, Request request,
                           HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
                throws IOException, ServletException {

            if (!request.getRemoteAddr().equals("127.0.0.1")) {
                httpServletResponse.setStatus(HttpStatus.BAD_REQUEST_400);
                request.setHandled(true);
                return;
            }

            (new Thread() {
                @Override
                public void run() {
                    // 销毁控制台
                    manager.destroy();

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    try {
                        Logger.i(StopHandler.class, "Stop Cube App Server # "
                                + server.getPlainPort() + "/" + server.getSecurePort());
                        server.stop();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            httpServletResponse.getWriter().write("Cube App Server");
            httpServletResponse.setStatus(HttpStatus.OK_200);
            request.setHandled(true);
        }
    }
}
