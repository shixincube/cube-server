/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
import java.net.URLDecoder;

/**
 * 内置账号数据信息。
 */
public class AccountBuildinHandler extends ContextHandler {

    private JSONArray buildinArray;

    public AccountBuildinHandler(String httpAllowOrigin, String httpsAllowOrigin) {
        super("/account/buildin/");
        setHandler(new Handler(httpAllowOrigin, httpsAllowOrigin));
    }

    protected class Handler extends CrossDomainHandler {

        public Handler(String httpAllowOrigin, String httpsAllowOrigin) {
            super();
            setHttpAllowOrigin(httpAllowOrigin);
            setHttpsAllowOrigin(httpsAllowOrigin);
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

            domain = URLDecoder.decode(domain, "UTF-8");
            zone = URLDecoder.decode(zone, "UTF-8");

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

            if (null != contactZone) {
                this.respond(response, HttpStatus.OK_200, contactZone.toCompactJSON());
            }
            else {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
            }

            this.complete();
        }
    }
}
