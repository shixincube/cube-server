/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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

            Token token = null;

            if (null != account) {
                token = AccountManager.getInstance().loginWithAccount(account, password, device);
            }
            else if (null != phone) {
                token = AccountManager.getInstance().loginWithPhoneNumber(phone, password, device);
            }

            if (null != token) {
                // 尝试绑定账号
                Account current = AccountManager.getInstance().bindAppletAccount(jsCode, phone, account, device, token);
                if (null == current) {
                    // 绑定失败
                    JSONObject responseData = new JSONObject();
                    responseData.put("code", StateCode.InvalidAccount.code);
                    this.respondOk(response, responseData);
                    return;
                }

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
                responseData.put("code", StateCode.NotFindAccount.code);
                this.respondOk(response, responseData);
            }
        }
    }
}
