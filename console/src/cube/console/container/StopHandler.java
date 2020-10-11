/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

package cube.console.container;

import cell.util.log.Logger;
import cube.console.Console;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
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

    private Server server;

    private Console console;

    public StopHandler(Server server, Console console) {
        super("/stop");
        this.setHandler(new Handler());
        this.server = server;
        this.console = console;
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
                    console.destroy();

                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    try {
                        ServerConnector conn = (ServerConnector) server.getConnectors()[0];
                        Logger.i(StopHandler.class, "Stop cube console server # " + conn.getPort());
                        server.stop();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            httpServletResponse.getWriter().write("Cube Console");
            httpServletResponse.setStatus(HttpStatus.OK_200);
            request.setHandled(true);
        }
    }
}
