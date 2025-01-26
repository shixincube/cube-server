/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
 * 支持跨域的 HTTP 处理器。
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

        if (null == origin) {
            return;
        }

        this.addAllowOrigin(origin);
    }

    public void setHttpsAllowOrigin(String origin) {
        this.httpsAllowOrigin = origin;

        if (null == origin) {
            return;
        }

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

        response.setHeader("Server", "Cube 3.0");

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
    }

    @Override
    public void doOptions(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setStatus(HttpStatus.OK_200);
        this.complete();
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
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        // 告诉请求端，哪些自定义的头字段可以被允许添加
        response.setHeader("Access-Control-Allow-Headers", "Access-Control-Allow-Headers,Authorization,User-Agent,Keep-Alive,Content-Type,X-Requested-With,X-CSRF-Token,AccessToken,Token,Content-Encoding,Content-Length,X-Requested-With");
        // 允许跨域请求的最长时间，指定时间内不需要发送预请求过来。
        response.setHeader("Access-Control-Max-Age", "3600");
//        response.setHeader("Access-Control-Expose-Headers", "Content-Length, Access-Control-Allow-Origin, Access-Control-Allow-Headers, Content-Type");
    }

    protected boolean isHttps() {
        return (this.baseRequest.getProtocol().toUpperCase().indexOf("HTTPS") >= 0);
    }
}
