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

public class KeepAlive extends ContextHandler {

    public KeepAlive() {
        super("/app/keepalive/");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        public Handler() {
            super();
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            String token = Helper.extractToken(request);
            if (null == token) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            App.ChannelInfo channelInfo = App.getInstance().getChannel(token);
            if (null == channelInfo) {
                this.respond(response, HttpStatus.NOT_FOUND_404);
                this.complete();
                return;
            }

            try {
                JSONObject data = this.readBodyAsJSONObject(request);
                if (null == data) {
                    this.respond(response, HttpStatus.BAD_REQUEST_400);
                    this.complete();
                    return;
                }

                String channelCode = data.getString("channel");
                if (Manager.getInstance().keepAliveChannel(channelCode)) {
                    // 更新本地数据
                    channelInfo.lastActivate = System.currentTimeMillis();
                    Helper.respondOk(this, response, data);
                }
                else {
                    Helper.respondFailure(this, response, HttpStatus.BAD_REQUEST_400);
                }
            } catch (Exception e) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
            }
        }
    }
}
