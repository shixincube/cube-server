/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * AIGC RESTful 接口基类。
 */
public abstract class AIGCHandler extends CrossDomainHandler {

    public AIGCHandler() {
        super();
    }

    protected String getApiToken(HttpServletRequest request) {
        String token = this.getLastRequestPath(request);
        if (null == token || token.contains("converse")) {
            try {
                token = request.getHeader("x-baize-api-token").trim();
            } catch (Exception e) {
                // Nothing
            }
        }
        return token;
    }

    protected JSONObject makeError(int stateCode) {
        JSONObject json = new JSONObject();
        JSONObject error = new JSONObject();
        switch (stateCode) {
            case HttpStatus.UNAUTHORIZED_401:
                error.put("message", "Invalid API token");
                error.put("reason", "INVALID_API_TOKEN");
                error.put("state", HttpStatus.UNAUTHORIZED_401);
                break;
            case HttpStatus.NOT_ACCEPTABLE_406:
                error.put("message", "URI format error");
                error.put("reason", "URI_FORMAT_ERROR");
                error.put("state", HttpStatus.NOT_ACCEPTABLE_406);
                break;
            case HttpStatus.BAD_REQUEST_400:
                error.put("message", "Server failure");
                error.put("reason", "SERVER_FAILURE");
                error.put("state", HttpStatus.BAD_REQUEST_400);
                break;
            case HttpStatus.FORBIDDEN_403:
                error.put("message", "Parameter error");
                error.put("reason", "PARAMETER_ERROR");
                error.put("state", HttpStatus.FORBIDDEN_403);
                break;
            default:
                break;
        }
        json.put("error", error);
        return json;
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
}
