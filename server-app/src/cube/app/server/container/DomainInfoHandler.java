/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.container;

import cube.app.server.Manager;
import cube.common.entity.AuthDomain;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 域信息。
 */
public class DomainInfoHandler extends ContextHandler {

    public DomainInfoHandler(String httpOrigin, String httpsOrigin) {
        super("/cube/domain/");
        setHandler(new Handler(httpOrigin, httpsOrigin));
    }

    protected class Handler extends CrossDomainHandler {
        public Handler(String httpOrigin, String httpsOrigin) {
            super();
            setHttpAllowOrigin(httpOrigin);
            setHttpsAllowOrigin(httpsOrigin);
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            Map<String, String> data = this.parseQueryStringParams(request);
            if (null == data || !data.containsKey("domain") || !data.containsKey("appKey")) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                return;
            }

            String domainName = data.get("domain");
            String appKey = data.get("appKey");
            AuthDomain authDomain = Manager.getInstance().getClient().getDomain(domainName, appKey);
            if (null == authDomain) {
                this.respond(response, HttpStatus.NOT_FOUND_404);
                return;
            }

            this.respondOk(response, authDomain.toJSON());
        }
    }
}
