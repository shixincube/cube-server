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
import cube.app.server.account.StateCode;
import cube.app.server.account.Token;
import cube.common.entity.Trace;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 账号登录。
 */
public class LoginHandler extends ContextHandler {

    private final static String COOKIE_NAME_TOKEN = "CubeAppToken";
    private final static String COOKIE_NAME_TRACE = "CubeTrace";

    public LoginHandler(String httpOrigin, String httpsOrigin) {
        super("/account/login/");
        setHandler(new Handler(httpOrigin, httpsOrigin));
    }

    protected class Handler extends CrossDomainHandler {

        public Handler(String httpOrigin, String httpsOrigin) {
            super();
            setHttpAllowOrigin(httpOrigin);
            setHttpsAllowOrigin(httpsOrigin);
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            // 尝试读取数据
            JSONObject data = this.readBodyAsJSONObject(request);

            if (null != data && data.has("device")) {
                String device = data.getString("device");
                Token token = null;

                // 读取登录参数
                if (data.has("account") && data.has("password")) {
                    String accountName = data.getString("account");
                    String password = data.getString("password");

                    token = AccountManager.getInstance().loginWithAccount(accountName, password, device);
                }
                else if (data.has("phone") && data.has("password")) {
                    String phoneNumber = data.getString("phone");
                    String password = data.getString("password");

                    token = AccountManager.getInstance().loginWithPhoneNumber(phoneNumber, password, device);
                }
                else if (data.has("token")) {
                    String tokenCode = data.getString("token");

                    token = AccountManager.getInstance().login(tokenCode);
                }
                else if (data.has("jsCode")) {
                    // 小程序登录
                    token = AccountManager.getInstance().loginWithAppletJSCode(data.getString("jsCode"), device);
                }

                JSONObject responseData = new JSONObject();

                if (null == token) {
                    responseData.put("code", StateCode.InvalidAccount.code);
                }
                else {
                    String trace = (new Trace(token.accountId)).toString();

                    responseData.put("code", StateCode.Success.code);
                    responseData.put("token", token.code);
                    responseData.put("trace", trace);
                    responseData.put("creation", token.creation);
                    responseData.put("expire", token.expire);

                    int maxAge = (int)(token.expire / 1000);
                    setCookie(response, COOKIE_NAME_TOKEN, token.code, maxAge);
                    setCookie(response, COOKIE_NAME_TRACE, trace, maxAge);
                }

                // 应答
                this.respondOk(response, responseData);
            }
            else {
                // 尝试读取 Cookie
                Cookie[] cookies = request.getCookies();
                if (null == cookies) {
                    respond(response, HttpStatus.BAD_REQUEST_400);
                    return;
                }

                String tokenCode = null;
                for (Cookie cookie : request.getCookies()) {
                    if (COOKIE_NAME_TOKEN.equals(cookie.getName())) {
                        tokenCode = cookie.getValue().trim();
                        break;
                    }
                }

                JSONObject responseData = null;
                Token token = AccountManager.getInstance().login(tokenCode);

                if (null == token) {
                    responseData = new JSONObject();
                    responseData.put("code", StateCode.InvalidToken.code);
                    responseData.put("token", tokenCode);

                    // 作废 Cookie
                    setCookie(response, COOKIE_NAME_TOKEN, "?", 1);
                    setCookie(response, COOKIE_NAME_TRACE, "?", 1);
                }
                else {
                    String trace = (new Trace(token.accountId)).toString();

                    responseData = new JSONObject();
                    responseData.put("code", StateCode.Success.code);
                    responseData.put("token", token.code);
                    responseData.put("trace", trace);
                    responseData.put("creation", token.creation);
                    responseData.put("expire", token.expire);
                }

                // 应答
                this.respondOk(response, responseData);
            }
        }
    }
}
