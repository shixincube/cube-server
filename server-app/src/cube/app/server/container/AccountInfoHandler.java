/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.container;

import cube.app.server.account.Account;
import cube.app.server.account.AccountManager;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 账号信息。
 */
public class AccountInfoHandler extends ContextHandler {

    public AccountInfoHandler(String httpOrigin, String httpsOrigin) {
        super("/account/info/");
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
            // 获取账号信息

            Map<String, String> data = this.parseQueryStringParams(request);

            JSONObject responseData = null;
            JSONArray responseArray = null;

            if (null != data && data.containsKey("id") && data.containsKey("token")) {
                String token = data.get("token");
                Long id = Long.parseLong(data.get("id"));

                if (AccountManager.getInstance().isValidToken(token)) {
                    Account account = AccountManager.getInstance().getAccount(id);
                    if (null != account) {
                        responseData = account.toCompactJSON();
                    }
                }
            }
            else if (null != data && data.containsKey("list") && data.containsKey("token")) {
                String token = data.get("token");

                if (AccountManager.getInstance().isValidToken(token)) {
                    String[] array = data.get("list").trim().split(",");
                    responseArray = new JSONArray();
                    for (String strId : array) {
                        Long accountId = Long.parseLong(strId.trim());
                        Account account = AccountManager.getInstance().getAccount(accountId);
                        if (null != account) {
                            responseArray.put(account.toCompactJSON());
                        }
                    }
                }
            }
            else if (null != data && data.containsKey("token")) {
                String token = data.get("token");
                Account account = AccountManager.getInstance().getOnlineAccount(token);
                if (null != account) {
                    // 返回完整信息
                    responseData = account.toFullJSON();
                }
            }

            if (null != responseData) {
                this.respondOk(response, responseData);
            }
            else if (null != responseArray) {
                this.respond(response, HttpStatus.OK_200, responseArray);
            }
            else {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
            }
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            // 修改账号信息

            JSONObject data = this.readBodyAsJSONObject(request);

            String token = data.getString("token");
            if (null == token) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                return;
            }

            Account account = AccountManager.getInstance().getOnlineAccount(token);
            if (null == account) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                return;
            }

            String newName = null;
            String newAvatar = null;

            if (data.has("name")) {
                newName = data.getString("name");
                if (newName.length() < 3) {
                    newName = null;
                }
                else {
                    account.name = newName;
                }
            }

            if (data.has("avatar")) {
                newAvatar = data.getString("avatar");
                if (newAvatar.length() < 2) {
                    newAvatar = null;
                }
                else {
                    account.avatar = newAvatar;
                }
            }

            account = AccountManager.getInstance().updateAccount(account.id, newName, newAvatar);
            this.respondOk(response, account.toCompactJSON());
        }
    }
}
