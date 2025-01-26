/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler.app;

import cell.util.log.Logger;
import cube.dispatcher.aigc.Manager;
import cube.dispatcher.aigc.handler.AIGCHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Inject extends ContextHandler {

    public Inject() {
        super("/app/inject/");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        public Handler() {
            super();
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            try {
                JSONObject data = this.readBodyAsJSONObject(request);
                String phone = data.getString("phone");
                String name = data.has("name") ? data.getString("name") : null;

                Manager.ContactToken contactToken = Manager.getInstance().checkOrInjectContactToken(phone, name);
                Helper.respondOk(this, response, contactToken.toJSON());
            } catch (Exception e) {
                Logger.w(Inject.class, "#doPost", e);
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
            }
        }
    }
}
