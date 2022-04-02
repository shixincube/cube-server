/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 支持跨域的处理器。
 */
public class CrossDomainHandler extends HttpHandler {

    private String httpAllowOrigin;

    private String httpsAllowOrigin;

    private List<String> allowOriginList;

    public CrossDomainHandler() {
        super();
        this.allowOriginList = new ArrayList<>();
    }

    public void setHttpAllowOrigin(String origin) {
        this.httpAllowOrigin = origin;
        this.addAllowOrigin(origin);
    }

    public void setHttpsAllowOrigin(String origin) {
        this.httpsAllowOrigin = origin;
        this.addAllowOrigin(origin);
    }

    public void addAllowOrigin(String origin) {
        if (this.allowOriginList.contains(origin)) {
            return;
        }

        this.allowOriginList.add(origin);
    }

    public void removeAllowOrigin(String origin) {
        this.allowOriginList.remove(origin);
    }

    @Override
    public void handle(String target, Request baseRequest,
                       HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        this.target = target;
        this.baseRequest = baseRequest;

        this.allowCrossDomain(response);

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

    public void doOptions(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setStatus(HttpStatus.OK_200);
    }

    protected void allowCrossDomain(HttpServletResponse response) {
        // 允许跨域
        String rootURL = this.baseRequest.getRootURL().toString();
        if (this.allowOriginList.contains(rootURL)) {
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Origin", rootURL);
        }
        else {
            if (null != this.httpAllowOrigin && !this.isHttps()) {
                response.setHeader("Access-Control-Allow-Credentials", "true");
                response.setHeader("Access-Control-Allow-Origin", this.httpAllowOrigin);
            }
            else if (null != this.httpsAllowOrigin && this.isHttps()) {
                response.setHeader("Access-Control-Allow-Credentials", "true");
                response.setHeader("Access-Control-Allow-Origin", this.httpsAllowOrigin);
            }
            else {
                response.setHeader("Access-Control-Allow-Origin", "*");
            }
        }

        // 允许的方法
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        // 告诉请求端，哪些自定义的头字段可以被允许添加
        response.setHeader("Access-Control-Allow-Headers", "Content-Type,Content-Encoding,Content-Length,X-Requested-With");
        // 允许跨域请求的最长时间，指定时间内不需要发送预请求过来。
        response.setHeader("Access-Control-Max-Age", "3600");
    }

    protected boolean isHttps() {
        return (this.baseRequest.getProtocol().toUpperCase().indexOf("HTTPS") >= 0);
    }
}
