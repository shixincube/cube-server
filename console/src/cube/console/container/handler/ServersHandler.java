/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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
import cube.console.mgmt.DispatcherServer;
import cube.console.mgmt.ServiceServer;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 服务器信息句柄。
 */
public class ServersHandler extends ContextHandler  {

    private Console console;

    public ServersHandler(Console console) {
        super("/servers");
        this.setHandler(new Handler());
        this.console = console;
    }

    protected class Handler extends CrossDomainHandler {

        public Handler() {
            super();
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            if (target.equals("/dispatcher")) {
                JSONObject data = new JSONObject();
                data.put("tag", console.getTag());

                JSONArray array = new JSONArray();
                List<DispatcherServer> list = console.getDispatcherManager().listDispatcherServers();
                if (null != list) {
                    for (DispatcherServer server : list) {
                        array.put(server.toJSON());
                    }
                }
                data.put("list", array);

                respondOk(response, data);
            }
            else if (target.equals("/service")) {
                JSONObject data = new JSONObject();
                data.put("tag", console.getTag());

                JSONArray array = new JSONArray();
                List<ServiceServer> list = console.getServiceManager().listServiceServers();
                if (null != list) {
                    for (ServiceServer server : list) {
                        array.put(server.toJSON());
                    }
                }
                data.put("list", array);

                respondOk(response, data);
            }
            else {
                // TODO
            }

            complete();
        }
    }
}
