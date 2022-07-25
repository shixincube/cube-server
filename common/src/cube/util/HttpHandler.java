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

        response.setHeader("Cube Server", "Cube 3.0");
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

                json.put(kv[0], URLDecoder.decode(kv[1], "UTF-8"));
            }
        }

        return json;
    }

    public Map<String, String> parseQueryStringParams(HttpServletRequest request) {
        String queryString = request.getQueryString();
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
            response.addHeader("Set-Cookie", name + "=" + URLEncoder.encode(value, "UTF-8") + "; max-age=" + maxAge +
                    "; SameSite=None; Secure" );
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
}
