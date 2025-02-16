/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cell.util.log.Logger;
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
            if (null == token || token.contains("converse")) {
                try {
                    token = request.getHeader("x-baize-api-token").trim();
                } catch (Exception e) {
                    Logger.w(this.getClass(), "#doPost", e);
                    this.respond(response, HttpStatus.NOT_ACCEPTABLE_406, this.makeError(HttpStatus.NOT_ACCEPTABLE_406));
                    this.complete();
                    return;
                }
            }

            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
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
                JSONObject relation = requestData.has("relation") ?
                        requestData.getJSONObject("relation") : null;

                JSONObject result = null;
                if (null != relations) {
                    result = Manager.getInstance().executePsychologyConversation(token, channelCode, relations, query);
                }
                else {
                    result = Manager.getInstance().executePsychologyConversation(token, channelCode,
                            context, relation, query);
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
                    this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                }
                this.complete();
            } catch (Exception e) {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
            }
        }

        private JSONObject makeError(int stateCode) {
            JSONObject json = new JSONObject();
            JSONObject error = new JSONObject();
            switch (stateCode) {
                case HttpStatus.UNAUTHORIZED_401:
                    error.put("message", "Invalid API token");
                    error.put("reason", "INVALID_API_TOKEN");
                    error.put("state", HttpStatus.UNAUTHORIZED_401);
                    break;
                case HttpStatus.NOT_ACCEPTABLE_406:
                    error.put("message", "URI format error");
                    error.put("reason", "URI_FORMAT_ERROR");
                    error.put("state", HttpStatus.NOT_ACCEPTABLE_406);
                    break;
                case HttpStatus.BAD_REQUEST_400:
                    error.put("message", "Server failure");
                    error.put("reason", "SERVER_FAILURE");
                    error.put("state", HttpStatus.BAD_REQUEST_400);
                    break;
                case HttpStatus.FORBIDDEN_403:
                    error.put("message", "Parameter error");
                    error.put("reason", "PARAMETER_ERROR");
                    error.put("state", HttpStatus.FORBIDDEN_403);
                    break;
                default:
                    break;
            }
            json.put("error", error);
            return json;
        }
    }
}
