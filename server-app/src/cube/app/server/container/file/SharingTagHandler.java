/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.container.file;

import cube.app.server.Manager;
import cube.app.server.account.Account;
import cube.app.server.account.AccountManager;
import cube.app.server.util.DefenseStrategy;
import cube.client.Client;
import cube.common.entity.SharingTag;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 分享标签句柄。
 */
public class SharingTagHandler extends ContextHandler {

    private DefenseStrategy strategy;

    public SharingTagHandler(String httpOrigin, String httpsOrigin) {
        super("/file/sharing/");
        this.strategy = new DefenseStrategy(100);
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
            Map<String, String> data = this.parseQueryStringParams(request);
            if (null == data || !data.containsKey("token") || !data.containsKey("sc")) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
                return;
            }

            String tokenCode = data.get("token");

            if (!strategy.verify(tokenCode)) {
                this.respond(response, HttpStatus.NOT_ACCEPTABLE_406);
                this.complete();
                return;
            }

            Account account = AccountManager.getInstance().getOnlineAccount(tokenCode);
            if (null == account) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            String sharingCode = data.get("sc");
            Client client = Manager.getInstance().getClient();
            SharingTag sharingTag = client.getFileStorage().getSharingTag(sharingCode);

            if (null == sharingCode) {
                this.respond(response, HttpStatus.NOT_FOUND_404);
                this.complete();
                return;
            }

            this.respondOk(response, sharingTag.toCompactJSON());
            this.complete();
        }
    }
}
