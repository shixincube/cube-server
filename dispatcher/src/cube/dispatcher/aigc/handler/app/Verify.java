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

/**
 * 验证令牌。
 */
public class Verify extends ContextHandler {

    public Verify() {
        super("/app/verify/");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        public Handler() {
            super();
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            JSONObject data = null;
            String token = null;

            try {
                data = this.readBodyAsJSONObject(request);
                if (null == data) {
                    this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                    this.complete();
                    return;
                }

                if (!data.has("token")) {
                    this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                    this.complete();
                    return;
                }

                token = data.getString("token");
                token = Manager.getInstance().checkAndGetToken(token, this.getDevice(request));
                String version = request.getHeader(HEADER_X_BAIZE_API_VERSION);
                if (null != version) {
                    if (null != token) {
                        data.put("verified", true);
                    }
                    else {
                        data.put("verified", false);
                    }
                    this.respondOk(response, data);
                    this.complete();
                }
                else {
                    if (null != token) {
                        data.put("token", token);
                        Helper.respondOk(this, response, data);
                    }
                    else {
                        Helper.respondFailure(this, response, HttpStatus.NOT_FOUND_404);
                    }
                }
            } catch (Exception e) {
                Logger.w(Verify.class, "#doPost", e);
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
            }
        }
    }
}
