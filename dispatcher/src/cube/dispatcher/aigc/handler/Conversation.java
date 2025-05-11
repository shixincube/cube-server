/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cell.util.log.Logger;
import cube.aigc.Consts;
import cube.aigc.ModelConfig;
import cube.common.entity.AIGCConversationResponse;
import cube.common.entity.GeneratingOption;
import cube.dispatcher.aigc.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 智能对话。
 */
public class Conversation extends ContextHandler {

    private final static String AI_NAME = "Baize";

    public Conversation() {
        super("/aigc/conversation/");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        public Handler() {
            super();
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getRequestPath(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            try {
                String code = request.getParameter("code");
                String strSN = request.getParameter("sn");
                long sn = Long.parseLong(strSN);

                AIGCConversationResponse conversationResponse = Manager.getInstance().queryConversation(token, code, sn);
                if (null == conversationResponse) {
                    this.respond(response, HttpStatus.NOT_FOUND_404);
                    this.complete();
                    return;
                }

                this.respondOk(response, conversationResponse.toCompactJSON());
                this.complete();
            } catch (Exception e) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
            }
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getRequestPath(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            String channelCode = null;
            String pattern = Consts.PATTERN_CHAT;
            String content = null;
            String unit = ModelConfig.BAIZE_NEXT_UNIT;
            GeneratingOption option = new GeneratingOption();
            int histories = 0;
            JSONArray records = null;
            boolean recordable = false;
            boolean networking = false;
            int searchTopK = 5;
            int searchFetchK = 50;
            JSONArray categories = null;
            try {
                JSONObject json = this.readBodyAsJSONObject(request);
                channelCode = json.getString("code");
                content = json.getString("content");

                if (json.has("unit")) {
                    unit = json.getString("unit");
                }

                if (json.has("option")) {
                    option = new GeneratingOption(json.getJSONObject("option"));
                }

                if (json.has("histories")) {
                    histories = json.getInt("histories");
                }

                if (json.has("records")) {
                    records = json.getJSONArray("records");
                }

                if (json.has("recordable")) {
                    recordable = json.getBoolean("recordable");
                }

                if (json.has("pattern")) {
                    pattern = json.getString("pattern");
                }

                if (json.has("categories")) {
                    categories = json.getJSONArray("categories");
                }

                if (json.has("networking")) {
                    networking = json.getBoolean("networking");
                }

                if (json.has("searchTopK")) {
                    searchTopK = json.getInt("searchTopK");
                }

                if (json.has("searchFetchK")) {
                    searchFetchK = json.getInt("searchFetchK");
                }
            } catch (Exception e) {
                Logger.e(Chat.class, "#doPost - Read body failed", e);
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            if (null == channelCode || null == content) {
                // 参数错误
                this.respond(response, HttpStatus.NOT_FOUND_404);
                this.complete();
                return;
            }

            // Conversation
            long sn = Manager.getInstance().executeConversation(token,
                    channelCode, pattern, content, histories, records, option);
            if (0 == sn) {
                // 发生错误
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
                return;
            }

            // Response
            JSONObject responseData = new JSONObject();
            responseData.put("participant", AI_NAME);
            responseData.put("sn", sn);
            responseData.put("timestamp", System.currentTimeMillis());

            this.respondOk(response, responseData);
            this.complete();
        }
    }
}
