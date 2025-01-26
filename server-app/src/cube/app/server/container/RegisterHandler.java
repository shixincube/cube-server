/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.container;

import cube.app.server.account.Account;
import cube.app.server.account.AccountManager;
import cube.app.server.account.StateCode;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;
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
            String domain = null;
            String accountName = null;
            String phone = null;
            String password = null;
            String nickname = null;
            String avatar = null;
            String captcha = null;
            String device = null;

            JSONObject data = this.readBodyAsJSONObject(request);
            if (data.has("domain")) {
                domain = data.getString("domain");
            }
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
            if (data.has("captcha")) {
                captcha = data.getString("captcha");
            }
            if (data.has("device")) {
                device = data.getString("device");
            }

            JSONObject responseData = null;
            if (null != device) {
                if (device.indexOf("Web") == 0 && null != captcha) {
                    // 校验验证码
                    if (AccountManager.getInstance().checkCaptchaCode(captcha)) {
                        // 验证码有效
                        Account account = AccountManager.getInstance().register(domain, accountName,
                                phone, password, nickname, avatar);
                        if (null != account) {
                            responseData = new JSONObject();
                            responseData.put("code", StateCode.Success.code);
                            responseData.put("account", account.toCompactJSON());
                        }
                        else {
                            responseData = new JSONObject();
                            responseData.put("code", StateCode.InvalidAccount.code);
                        }
                    }
                    else {
                        // 无效验证码
                        responseData = new JSONObject();
                        responseData.put("code", StateCode.InvalidCaptcha.code);
                    }
                }
                else {
                    responseData = new JSONObject();
                    responseData.put("code", StateCode.NotAllowed.code);
                }
            }
            else if (null != accountName && null != password && null != nickname && null != avatar) {
                Account account = AccountManager.getInstance().registerWithAccountName(domain, accountName, password, nickname, avatar);
                if (null != account) {
                    responseData = new JSONObject();
                    responseData.put("code", StateCode.Success.code);
                    responseData.put("account", account.toCompactJSON());
                }
                else {
                    responseData = new JSONObject();
                    responseData.put("code", StateCode.InvalidAccount.code);
                }
            }
            else if (null != phone && null != password) {
                Account account = AccountManager.getInstance().registerWithPhoneNumber(domain, phone, password, nickname, avatar);
                if (null != account) {
                    responseData = new JSONObject();
                    responseData.put("code", StateCode.Success.code);
                    responseData.put("account", account.toCompactJSON());
                }
                else {
                    responseData = new JSONObject();
                    responseData.put("code", StateCode.InvalidAccount.code);
                }
            }

            if (null != responseData) {
                this.respondOk(response, responseData);
            }
            else {
                this.respond(response, HttpStatus.FORBIDDEN_403);
            }
        }
    }
}
