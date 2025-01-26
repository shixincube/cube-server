/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.container.handler;

import cube.console.Console;
import cube.console.Utils;
import cube.report.JVMReport;
import cube.report.PerformanceReport;
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
 * 服务器报告信息。
 */
public class ServerReportHandler extends ContextHandler {

    private Console console;

    public ServerReportHandler(Console console) {
        super("/server-report");
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

            String query = URLDecoder.decode(httpServletRequest.getQueryString(), "UTF-8");
            Map<String, String> params = Utils.parseQueryStringParams(query);

            httpServletResponse.setContentType("application/json");

            String name = params.get("name");
            String report = params.get("report");
            long time = Long.parseLong(params.get("time"));

            if (null != name && null != report) {
                if (report.equals(JVMReport.NAME)) {
                    int num = Integer.parseInt(params.get("num"));

                    List<JVMReport> list = console.queryJVMReport(name, num, time);

                    JSONArray result = new JSONArray();
                    for (JVMReport r : list) {
                        result.put(r.toJSON());
                    }

                    JSONObject response = new JSONObject();
                    response.put("name", name);
                    response.put("list", result);

                    httpServletResponse.getWriter().write(response.toString());
                }
                else if (report.equals(PerformanceReport.NAME)) {
                    PerformanceReport perfReport = null;

                    if (time == 0) {
                        perfReport = console.queryLastPerformanceReport(name);
                    }
                    else {
                        perfReport = console.queryPerformanceReport(name, time);
                    }

                    JSONObject response = new JSONObject();

                    if (null == perfReport) {
                        response.put("name", name);
                    }
                    else {
                        boolean detail = false;
                        if (params.containsKey("detail")) {
                            detail = params.get("detail").equalsIgnoreCase("true");
                        }

                        response.put("name", name);
                        response.put("report", detail ? perfReport.toDetailJSON() : perfReport.toCompactJSON());
                    }

                    httpServletResponse.getWriter().write(response.toString());
                }
            }

            httpServletResponse.setStatus(HttpStatus.OK_200);
            request.setHandled(true);
        }
    }
}
