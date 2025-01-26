/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cube.aigc.attachment.ui.Event;
import cube.dispatcher.aigc.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 提交事件。
 */
public class SubmitEvent extends ContextHandler {

    public SubmitEvent() {
        super("/aigc/component/event/");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        public Handler() {
            super();
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getLastRequestPath(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            try {
                JSONObject eventJson = this.readBodyAsJSONObject(request);
                Event event = new Event(eventJson);
                JSONObject result = Manager.getInstance().submitEvent(token, event);
                if (null != result) {
                    this.respondOk(response, result);
                    this.complete();
                }
                else {
                    this.respond(response, HttpStatus.BAD_REQUEST_400);
                    this.complete();
                }
            } catch (Exception e) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
            }
        }
    }
}
