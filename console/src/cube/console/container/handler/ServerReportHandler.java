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
import cube.console.Utils;
import cube.report.JVMReport;
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
                    try {
                        response.put("name", name);
                        response.put("list", result);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    httpServletResponse.getWriter().write(response.toString());
                }
            }

            httpServletResponse.setContentType("application/json");
            httpServletResponse.setStatus(HttpStatus.OK_200);
            request.setHandled(true);
        }
    }
}
