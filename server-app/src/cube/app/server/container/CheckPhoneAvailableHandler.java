/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
