/*
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
import cube.console.mgmt.DispatcherManager;
import cube.console.mgmt.ServiceManager;
import cube.console.mgmt.ServiceServer;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 请求进行部署。
 */
public class DeployHandler extends ContextHandler {

    private Console console;

    public DeployHandler(Console console) {
        super("/deploy");
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
        public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            if (!Handlers.checkCookie(request, console)) {
                respond(response, HttpStatus.UNAUTHORIZED_401);
                complete();
                return;
            }

            if (target.equals("/dispatcher")) {
                DispatcherManager manager = console.getDispatcherManager();
                String tag = console.getTag();

                JSONObject data = new JSONObject();
                data.put("tag", tag);

                String deployPath = manager.getDefaultDeployPath();
                if (null != deployPath) {
                    String cellConfigFile = manager.getDefaultCellConfigFile();
                    String propertiesFile = manager.getDefaultPropertiesFile();

                    data.put("deployPath", deployPath);
                    data.put("cellConfigFile", cellConfigFile);
                    data.put("propertiesFile", propertiesFile);
                }

                respondOk(response, data);
            }
            else if (target.equals("/service")) {
                ServiceManager manager = console.getServiceManager();
                String tag = console.getTag();

                JSONObject data = new JSONObject();
                data.put("tag", tag);

                String deployPath = manager.getDefaultDeployPath();
                if (null != deployPath) {
                    data.put("deployPath", deployPath);
                    data.put("configPath", manager.getDefaultConfigPath());
                    data.put("celletsPath", manager.getDefaultCelletsPath());
                }

                respondOk(response, data);
            }

            complete();
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            if (!Handlers.checkCookie(request, console)) {
                respond(response, HttpStatus.UNAUTHORIZED_401);
                complete();
                return;
            }

            String deployPath = request.getParameter("deployPath");


            complete();
        }
    }
}
