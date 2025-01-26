/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.container.handler;

import cube.console.Console;
import cube.console.Utils;
import cube.report.LogLine;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

/**
 * 服务器日志信息。
 */
public class ServerLogHandler extends ContextHandler {

    private Console console;

    public ServerLogHandler(Console console) {
        super("/log");
        setHandler(new Handler());
        this.console = console;
    }

    protected class Handler extends AbstractHandler {

        public Handler() {
            super();
        }

        @Override
        public void handle(String target, Request request,
                           HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
                throws IOException, ServletException {

            if (target.equals("/console")) {
                String[] array = httpServletRequest.getQueryString().split("=");
                long start = Long.parseLong(array[1]);
                List<LogLine> list = console.queryConsoleLogs(start, 20);
                JSONArray result = new JSONArray();
                for (LogLine line : list) {
                    result.put(line.toJSON());
                }

                JSONObject response = new JSONObject();
                try {
                    response.put("lines", result);
                    if (list.isEmpty()) {
                        response.put("last", start);
                    }
                    else {
                        response.put("last", list.get(list.size() - 1).time);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                httpServletResponse.getWriter().write(response.toString());
            }
            else if (target.equals("/server")) {
                String query = URLDecoder.decode(httpServletRequest.getQueryString(), "UTF-8");
                Map<String, String> params = Utils.parseQueryStringParams(query);

                httpServletResponse.setContentType("application/json");

                String name = params.get("name");
                long start = Long.parseLong(params.get("start"));

                if (null != name) {
                    List<LogLine> list = console.queryLogs(name, start, 40);

                    JSONArray result = new JSONArray();
                    for (LogLine line : list) {
                        result.put(line.toJSON());
                    }

                    JSONObject response = new JSONObject();
                    try {
                        response.put("name", name);
                        response.put("lines", result);
                        if (list.isEmpty()) {
                            response.put("last", start);
                        }
                        else {
                            response.put("last", list.get(list.size() - 1).time);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    httpServletResponse.getWriter().write(response.toString());
                }
            }

            httpServletResponse.setStatus(HttpStatus.OK_200);
            request.setHandled(true);
        }
    }
}
