/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler.app;

import cube.aigc.ConversationRequest;
import cube.aigc.ConversationResponse;
import cube.dispatcher.aigc.Manager;
import cube.dispatcher.aigc.handler.AIGCHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * For Web
 */
public class Chat extends ContextHandler {

    public Chat() {
        super("/app/chat/");
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
                token = this.getApiToken(request);
                if (null == token) {
                    this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                    this.complete();
                    return;
                }
            }

            if (!Manager.getInstance().checkToken(token, this.getDevice(request))) {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
                return;
            }

            JSONObject data = null;
            try {
                data = this.readBodyAsJSONObject(request);
            } catch (Exception e) {
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
                return;
            }

            ConversationRequest convRequest = new ConversationRequest(data);
            List<ConversationResponse> convResponseList = App.getInstance().requestConversation(token, convRequest);
            if (null == convResponseList || convResponseList.isEmpty()) {
                this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                this.complete();
                return;
            }

            JSONArray responses = new JSONArray();
            for (ConversationResponse cr : convResponseList) {
                responses.put(cr.toJSON());
            }

            JSONObject responseData = new JSONObject();
            responseData.put("responses", responses);
            responseData.put("status", "Success");

            this.respondOk(response, responseData);
            this.complete();
        }
    }
}
