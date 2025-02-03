/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cube.dispatcher.aigc.Manager;
import cube.dispatcher.util.FileLabels;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 心理学领域对话。
 */
public class PsychologyConversation extends ContextHandler {

    public PsychologyConversation() {
        super("/aigc/psychology/converse");
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

            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            try {
                JSONObject requestData = this.readBodyAsJSONObject(request);
                String channelCode = requestData.getString("channelCode");
                String query = requestData.getString("query");
                JSONArray relations = requestData.has("relations") ?
                        requestData.getJSONArray("relations") : null;
                JSONObject context = requestData.has("context") ?
                        requestData.getJSONObject("context") : null;

                JSONObject result = null;
                if (null != relations) {
                    result = Manager.getInstance().executePsychologyConversation(token, channelCode, relations, query);
                }
                else {
                    result = Manager.getInstance().executePsychologyConversation(token, channelCode, context, query);
                }

                if (null != result) {
                    if (result.has("queryFileLabels")) {
                        JSONArray array = result.getJSONArray("queryFileLabels");
                        for (int i = 0; i < array.length(); ++i) {
                            FileLabels.reviseFileLabel(array.getJSONObject(i), token,
                                    Manager.getInstance().getPerformer().getExternalHttpEndpoint(),
                                    Manager.getInstance().getPerformer().getExternalHttpsEndpoint());
                        }
                    }
                    if (result.has("answerFileLabels")) {
                        JSONArray array = result.getJSONArray("answerFileLabels");
                        for (int i = 0; i < array.length(); ++i) {
                            FileLabels.reviseFileLabel(array.getJSONObject(i), token,
                                    Manager.getInstance().getPerformer().getExternalHttpEndpoint(),
                                    Manager.getInstance().getPerformer().getExternalHttpsEndpoint());
                        }
                    }

                    this.respondOk(response, result);
                }
                else {
                    this.respond(response, HttpStatus.BAD_REQUEST_400);
                }
                this.complete();
            } catch (Exception e) {
                this.respond(response, HttpStatus.NOT_ACCEPTABLE_406);
                this.complete();
            }
        }
    }
}
