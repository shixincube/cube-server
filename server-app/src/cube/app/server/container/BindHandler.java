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

import cube.app.server.account.Account;
import cube.app.server.account.AccountManager;
import cube.app.server.account.StateCode;
import cube.app.server.account.Token;
import cube.common.entity.Trace;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 账号登录。
 */
public class BindHandler extends ContextHandler {

    public BindHandler(String httpOrigin, String httpsOrigin) {
        super("/account/bind/");
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

            String device = data.has("device") ? data.getString("device") : null;
            String jsCode = data.has("jsCode") ? data.getString("jsCode") : null;
            String phone = data.has("phone") ? data.getString("phone") : null;
            String account = data.has("account") ? data.getString("account") : null;
            String password = data.has("password") ? data.getString("password") : null;

            // 尝试绑定账号
            Account current = AccountManager.getInstance().bindAppletAccount(jsCode, phone, account, device);
            if (null == current) {
                // 绑定失败
                JSONObject responseData = new JSONObject();
                responseData.put("code", StateCode.NotFindAccount);
                this.respondOk(response, responseData);
                return;
            }

            Token token = null;

            if (null != account) {
                token = AccountManager.getInstance().loginWithAccount(account, password, device);
            }
            else if (null != phone) {
                token = AccountManager.getInstance().loginWithPhoneNumber(phone, password, device);
            }

            if (null != token) {
                String trace = (new Trace(token.accountId)).toString();

                JSONObject responseData = new JSONObject();
                responseData.put("code", StateCode.Success.code);
                responseData.put("token", token.code);
                responseData.put("trace", trace);
                responseData.put("creation", token.creation);
                responseData.put("expire", token.expire);

                this.respondOk(response, responseData);
            }
            else {
                JSONObject responseData = new JSONObject();
                responseData.put("code", StateCode.InvalidToken.code);
                this.respondOk(response, responseData);
            }
        }
    }
}
