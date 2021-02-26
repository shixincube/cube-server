/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

import cube.console.Console;
import cube.console.container.Handlers;
import cube.console.mgmt.ServiceServer;
import cube.console.mgmt.UserToken;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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

            complete();
        }
    }
}
