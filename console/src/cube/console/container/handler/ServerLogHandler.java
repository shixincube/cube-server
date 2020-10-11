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

package cube.console.container.handler;

import cell.util.json.JSONArray;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cube.console.Console;
import cube.console.Utils;
import cube.report.LogLine;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
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

                String name = params.get("name");
                long start = Long.parseLong(params.get("start"));

                if (null != name) {
                    List<LogLine> list = console.queryLogs(name, start, 20);

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
            }

            httpServletResponse.setContentType("application/json");
            httpServletResponse.setStatus(HttpStatus.OK_200);
            request.setHandled(true);
        }
    }
}
