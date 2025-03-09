/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cell.util.log.Logger;
import cube.aigc.Consts;
import cube.aigc.ModelConfig;
import cube.common.entity.AIGCChannel;
import cube.common.entity.GeneratingOption;
import cube.common.entity.GeneratingRecord;
import cube.dispatcher.aigc.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 对话。
 */
public class Chat extends ContextHandler {

    private final static String AI_NAME = "Baize";

    public Chat() {
        super("/aigc/chat/");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        public Handler() {
            super();
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getApiToken(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            String channelCode = null;
            String content = null;
            String pattern = Consts.PATTERN_CHAT;
            String unit = ModelConfig.BAIZE_NEXT_UNIT;
            GeneratingOption option = new GeneratingOption();
            int histories = 0;
            JSONArray records = null;
            boolean recordable = true;
            boolean searchable = true;
            boolean networking = false;
            int searchTopK = 5;
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

                if (json.has("searchable")) {
                    searchable = json.getBoolean("searchable");
                }

                if (json.has("networking")) {
                    networking = json.getBoolean("networking");
                }

                if (json.has("searchTopK")) {
                    searchTopK = json.getInt("searchTopK");
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

            // Chat
            Manager.ChatFuture future = Manager.getInstance().chat(token, channelCode, pattern, content, unit, option,
                    histories, records, recordable, searchable, networking, categories, searchTopK);
            if (null == future) {
                // 发生错误
                this.respond(response, HttpStatus.NOT_ACCEPTABLE_406);
                this.complete();
            }
            else if (future.end) {
                // 已结束
                if (null != future.record) {
                    // Record 转结果
                    GeneratingRecord record = future.record;
                    JSONObject responseData = new JSONObject();
                    responseData.put("sn", record.sn);
                    responseData.put("participant", unit);
                    responseData.put("timestamp", record.timestamp);
                    responseData.put("pattern", pattern);
                    responseData.put("content", null != record.answer ? record.answer : "");
                    responseData.put("thought", null != record.thought ? record.thought : "");
                    if (null != record.context) {
                        responseData.put("context", record.context.toJSON());
                    }
                    if (null != record.answerFileLabels) {
                        responseData.put("fileLabels", record.outputAnswerFileLabelArray());
                    }

                    this.respondOk(response, responseData);
                    this.complete();
                }
                else if (null != future.knowledgeResult) {
                    // Knowledge Result 转结果
                    JSONObject responseData = new JSONObject();
                    responseData.put("sn", future.knowledgeResult.record.sn);
                    responseData.put("participant", AI_NAME);
                    responseData.put("timestamp", future.knowledgeResult.record.timestamp);
                    responseData.put("pattern", pattern);
                    responseData.put("content", null != future.knowledgeResult.record.answer ? future.knowledgeResult.record.answer : "");
                    responseData.put("knowledgeSources", future.knowledgeResult.sourcesToArray());

                    this.respondOk(response, responseData);
                    this.complete();
                }
                else {
                    // 发生错误
                    this.respond(response, HttpStatus.BAD_REQUEST_400);
                    this.complete();
                }
            }
            else {
                // 正在处理
                AIGCChannel channel = future.channel;

                // Channel 转结果
                JSONObject responseData = channel.toInfo();
                if (responseData.has("authToken")) {
                    responseData.remove("authToken");
                }
                if (responseData.has("lastRecord")) {
                    responseData.remove("lastRecord");
                }

                responseData.put("message", "正在思考中，请稍作等待……");

                this.respondOk(response, responseData);
                this.complete();
            }
        }
    }
}
