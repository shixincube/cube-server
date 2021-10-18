/*
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

import cube.app.server.account.AccountManager;
import cube.app.server.account.StateCode;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 检查手机号码。
 */
public class CheckPhoneAvailableHandler extends ContextHandler {

    public CheckPhoneAvailableHandler(String httpOrigin, String httpsOrigin) {
        super("/account/check_phone_available/");
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
            // 读取数据
            JSONObject data = this.readBodyAsJSONObject(request);

            String regionCode = null;
            String phoneNumber = null;
            boolean verificationCodeRequired = false;

            if (data.has("regionCode") && data.has("phoneNumber")) {
                regionCode = data.getString("regionCode");
                phoneNumber = data.getString("phoneNumber");
            }

            if (null == regionCode || null == phoneNumber) {
                JSONObject responseData = new JSONObject();
                responseData.put("code", StateCode.DataError.code);
                responseData.put("verificationCodeRequired", verificationCodeRequired);
                // 应答
                this.respondOk(response, responseData);
                return;
            }

            if (data.has("verificationCodeRequired")) {
                verificationCodeRequired = data.getBoolean("verificationCodeRequired");
            }

            boolean result = AccountManager.getInstance().checkPhoneAvailable(regionCode, phoneNumber, verificationCodeRequired);

            JSONObject responseData = new JSONObject();
            responseData.put("code", result ? StateCode.Success.code : StateCode.NotAllowed.code);
            responseData.put("verificationCodeRequired", verificationCodeRequired);
            // 应答
            this.respondOk(response, responseData);
        }
    }
}
