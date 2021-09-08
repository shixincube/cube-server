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

package cube.app.server.container;

import cube.app.server.account.Account;
import cube.app.server.account.AccountManager;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 注册账号。
 */
public class RegisterHandler extends ContextHandler {

    public RegisterHandler(String httpOrigin, String httpsOrigin) {
        super("/account/register/");
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
            String accountName = null;
            String phone = null;
            String password = null;
            String nickname = null;
            String avatar = null;

            JSONObject data = this.readBodyAsJSONObject(request);
            if (data.has("account")) {
                accountName = data.getString("account");
            }
            if (data.has("phone")) {
                phone = data.getString("phone");
            }
            if (data.has("password")) {
                password = data.getString("password");
            }
            if (data.has("nickname")) {
                nickname = data.getString("nickname");
            }
            if (data.has("avatar")) {
                avatar = data.getString("avatar");
            }

            JSONObject responseData = null;
            if (null != accountName && null != password && null != nickname && null != avatar) {
                Account account = AccountManager.getInstance().registerWithAccountName(accountName, password, nickname, avatar);
                if (null != account) {
                    responseData = account.toCompactJSON();
                }
            }
            else if (null != phone && null != password) {
                responseData = new JSONObject();
            }

            if (null != responseData) {
                this.respondOk(response, responseData);
            }
            else {

            }
        }
    }
}
