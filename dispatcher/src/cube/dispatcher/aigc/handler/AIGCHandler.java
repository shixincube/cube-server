/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

/**
 * AIGC RESTful 接口基类。
 */
public abstract class AIGCHandler extends CrossDomainHandler {

    public AIGCHandler() {
        super();
    }

    protected String getApiToken(HttpServletRequest request) {
        String token = this.getLastRequestPath(request);
        if (null == token || token.length() < 32) {
            try {
                token = request.getHeader("x-baize-api-token").trim();
            } catch (Exception e) {
                // Nothing
            }
        }
        return token;
    }

    protected String getRequestPath(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path.length() < 2) {
            return null;
        }

        path = path.substring(1).trim();
        int index = path.indexOf("/");
        if (index > 0) {
            path = path.substring(0, index);
        }
        return path;
    }

    protected String getLastRequestPath(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path.length() < 2) {
            return null;
        }

        path = path.substring(1).trim();
        String[] paths = path.split("/");
        path = paths[paths.length - 1];
        return path;
    }

    protected void responseRefreshPage(HttpServletResponse response) {
        String html = "<!DOCTYPE html>\n" +
                "<html>" +
                "<head><meta http-equiv=\"refresh\" content=\"5\"/><title></title></head>" +
                "<body><script type=\"text/javascript\">" +
                "setTimeout(function() {" +
                " window.location.reload(); " +
                "}, 5000);" +
                "</script></body>" +
                "</html>\n";
        response.setContentType("text/html");
        response.setStatus(HttpStatus.OK_200);
        try {
            response.getOutputStream().write(html.getBytes(StandardCharsets.UTF_8));
            response.getOutputStream().flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
