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

package cube.app.server.container;

import cube.app.server.account.AccountManager;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 升级账号。
 */
public class UpgradeHandler extends ContextHandler {

    public UpgradeHandler(String httpOrigin, String httpsOrigin) {
        super("/account/upgrade/");
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
            if (request.getRequestURI().indexOf("favicon") > 0) {
                byte[] data = Files.readAllBytes(Paths.get("assets/favicon.png"));
                response.setContentType("image/png");
                response.getOutputStream().write(data);
                response.getOutputStream().close();
                response.setStatus(HttpStatus.OK_200);
                this.complete();
                return;
            }

            String url = AccountManager.getInstance().getUpgradeExternalURL();
            if (null == url) {
                byte[] data = Files.readAllBytes(Paths.get("assets/upgrade.html"));
                response.setContentType("text/html");
                response.setCharacterEncoding("UTF-8");
                response.getOutputStream().write(data);
                response.getOutputStream().close();
                response.setStatus(HttpStatus.OK_200);
                this.complete();
                return;
            }

            String token = null;
            Cookie[] cookies = request.getCookies();
            if (null != cookies) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equalsIgnoreCase("CubeAppToken")) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }

            if (null != token) {
                if (url.indexOf("?") > 0) {
                    url += "&_t=" + token;
                }
                else {
                    url += "?_t=" + token;
                }
            }

            response.setStatus(HttpStatus.MOVED_PERMANENTLY_301);
            response.setHeader("Location", url);
            this.complete();
        }
    }
}
