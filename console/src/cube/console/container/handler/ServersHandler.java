/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
