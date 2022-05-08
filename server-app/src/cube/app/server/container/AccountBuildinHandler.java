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

import cube.app.server.Manager;
import cube.app.server.account.Account;
import cube.app.server.account.AccountManager;
import cube.client.Client;
import cube.common.entity.Contact;
import cube.common.entity.ContactZone;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 内置账号数据信息。
 */
public class AccountBuildinHandler extends ContextHandler {

    private JSONArray buildinArray;

    public AccountBuildinHandler(String httpOrigin, String httpsOrigin) {
        super("/account/buildin/");
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
            if (null == buildinArray) {
                Long[] idArray = new Long[] { 50001001L, 50001002L, 50001003L, 50001004L, 50001005L };
                buildinArray = new JSONArray();
                for (Long id : idArray) {
                    Account account = AccountManager.getInstance().getAccount(id);
                    if (null != account) {
                        buildinArray.put(account.toCompactJSON());
                    }
                }
            }

            JSONObject data = new JSONObject();
            data.put("list", buildinArray);
            data.put("total", buildinArray.length());

            this.respond(response, HttpStatus.OK_200, data);
            this.complete();
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            String id = request.getParameter("id");
            String domain = request.getParameter("domain");
            String zone = request.getParameter("zone");

            if (null == id || null == domain || null == zone) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            ContactZone contactZone = null;

            Client client = Manager.getInstance().getClient();

            Contact contact = new Contact(Long.parseLong(id), domain);

            Long[] idArray = new Long[] { 50001001L, 50001002L, 50001003L, 50001004L, 50001005L };
            for (Long accountId : idArray) {
                Account account = AccountManager.getInstance().getAccount(accountId);
                if (null != account) {
                    Contact participant = new Contact(account.id, domain, account.name);
                    contactZone = client.addParticipantToZoneByForce(contact, zone, participant);
                }
            }

            this.respond(response, HttpStatus.OK_200, contactZone.toCompactJSON());
            this.complete();
        }
    }
}
