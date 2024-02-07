/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

package cube.app.server.container;

import cube.app.server.account.AccountManager;
import cube.app.server.version.AppVersion;
import cube.app.server.version.VersionManager;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 通知处理。
 */
public class VersionHandler extends ContextHandler {

    public VersionHandler(String httpOrigin, String httpsOrigin) {
        super("/version/");
        setHandler(new Handler(httpOrigin, httpsOrigin));
    }

    protected class Handler extends CrossDomainHandler {
        public Handler(String httpOrigin, String httpsOrigin) {
            super();
            setHttpAllowOrigin(httpOrigin);
            setHttpsAllowOrigin(httpsOrigin);
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            Map<String, String> data = this.parseQueryStringParams(request);
            if (null == data || !data.containsKey("token") || !data.containsKey("device")) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                return;
            }

            String token = data.get("token");
            if (!AccountManager.getInstance().isValidToken(token)) {
                // 无效令牌
                this.respond(response, HttpStatus.FORBIDDEN_403);
                return;
            }

            String device = data.get("device");

            AppVersion version = VersionManager.getInstance().getVersion(device);
            if (null == version) {
                version = new AppVersion(device, 3, 0, 0, false);
            }

            JSONObject responseData = new JSONObject();
            responseData.put("device", device);
            responseData.put("version", version.toJSON());

            this.respondOk(response, responseData);
        }
    }
}
