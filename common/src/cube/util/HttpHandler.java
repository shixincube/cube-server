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

package cube.util;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * HTTP 请求处理句柄。
 */
public abstract class HttpHandler extends AbstractHandler {

    protected String target;

    protected Request baseRequest;

    public HttpHandler() {
        super();
    }

    @Override
    public void handle(String target, Request baseRequest,
                       HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        this.target = target;
        this.baseRequest = baseRequest;

        String method = request.getMethod().toUpperCase();
        if (method.equals("GET")) {
            doGet(request, response);
        }
        else if (method.equals("POST")) {
            doPost(request, response);
        }
        else if (method.equals("OPTIONS")) {
            doOptions(request, response);
        }

        response.setHeader("Server", "Cube 3.0");
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // Nothing
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // Nothing
    }

    public void doOptions(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // Nothing
    }

    public void respondOk(HttpServletResponse response, JSONObject data) {
        this.respond(response, HttpStatus.OK_200, data);
    }

    public void respond(HttpServletResponse response, int status) {
        this.respond(response, status, new JSONObject());
    }

    public void respond(HttpServletResponse response, int status, JSONObject data) {
        response.setStatus(status);

        if (null != data) {
            response.setContentType("application/json");
            try {
                response.getWriter().write(data.toString());
                response.getWriter().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void complete() {
        this.baseRequest.setHandled(true);
    }
}
