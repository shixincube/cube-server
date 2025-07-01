/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler.app;

import cube.dispatcher.aigc.Manager;
import cube.dispatcher.aigc.handler.AIGCHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ASCIIArt extends ContextHandler {

    public ASCIIArt() {
        super("/app/asciiart/");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        public Handler() {
            super();
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getApiToken(request);
            if (null == token) {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
                return;
            }

            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
                return;
            }

            JSONObject data = Manager.getInstance().getASCIIArt(token);
            if (null == data) {
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
                return;
            }

            this.respondOk(response, data);
            this.complete();
        }
    }
}
