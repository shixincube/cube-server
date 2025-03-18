/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.Logger;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP 请求处理句柄。
 */
public abstract class HttpHandler extends AbstractHandler {

    public final static String HEADER_X_BAIZE_API_TOKEN = "x-baize-api-token";
    public final static String HEADER_X_BAIZE_API_CLIENT = "x-baize-api-client";
    public final static String HEADER_X_BAIZE_API_VERSION = "x-baize-api-version";

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

        response.setHeader("Cube Server", "Cube 3.0");

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

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // Nothing
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // Nothing
    }

    public void doOptions(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // Nothing
    }

    /**
     *
     * @param request
     * @return 如果没有数据返回 <code>null</code> 值。
     * @throws IOException
     */
    public JSONObject readBodyAsJSONObject(HttpServletRequest request) throws IOException {
        ServletInputStream is = request.getInputStream();
        FlexibleByteBuffer buffer = new FlexibleByteBuffer(512);
        byte[] buf = new byte[512];
        int len = 0;
        while ((len = is.read(buf)) > 0) {
            buffer.put(buf, 0, len);
        }

        buffer.flip();

        if (buffer.limit() == 0) {
            // 没有数据
            return null;
        }

        JSONObject json = null;

        String data = new String(buffer.array(), 0, buffer.limit(), Charset.forName("UTF-8"));
        data = data.trim();

        if (data.startsWith("{") && data.endsWith("}")) {
            try {
                json = new JSONObject(data);
            } catch (JSONException e) {
                Logger.w(this.getClass(), "", e);
            }
        }
        else if (data.startsWith("[") && data.endsWith("]")) {
            Logger.e(this.getClass(), "Unsupported JSON array type");
        }
        else {
            json = new JSONObject();
            String[] params = data.split("&");
            for (String param : params) {
                String[] kv = param.split("=");
                if (kv.length != 2) {
                    continue;
                }

                String k = URLDecoder.decode(kv[0].trim().replaceAll("%(?![0-9a-fA-F]{2})", "%25"), "UTF-8");
                String v = URLDecoder.decode(kv[1].trim().replaceAll("%(?![0-9a-fA-F]{2})", "%25"), "UTF-8");
                json.put(k, v);
            }
        }

        return json;
    }

    public Map<String, String> parseQueryStringParams(HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (null == queryString) {
            return null;
        }

        Map<String, String> result = new HashMap<>();
        String[] array = queryString.split("&");

        for (String pair : array) {
            String[] param = pair.split("=");
            if (param.length != 2) {
                continue;
            }

            String value = param[1];
            try {
                value = URLDecoder.decode(param[1], "UTF-8");
            } catch (Exception e) {
                // Nothing
            }
            result.put(param[0], value);
        }

        return result;
    }

    public void setCookie(HttpServletResponse response, String name, String value, int maxAge) {
        try {
            response.addHeader("Set-Cookie", name + "=" + URLEncoder.encode(value, "UTF-8") + "; max-age=" + maxAge);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
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

    public void respond(HttpServletResponse response, int status, JSONArray data) {
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
            case HttpStatus.NOT_FOUND_404:
                error.put("message", "Can not find data");
                error.put("reason", "CAN_NOT_FIND_DATA");
                error.put("state", HttpStatus.NOT_FOUND_404);
                break;
            case HttpStatus.REQUEST_TIMEOUT_408:
                error.put("message", "Request timeout");
                error.put("reason", "REQUEST_TIMEOUT");
                error.put("state", HttpStatus.REQUEST_TIMEOUT_408);
                break;
            default:
                break;
        }
        json.put("error", error);
        return json;
    }
}
