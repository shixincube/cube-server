/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.dispatcher.aigc.handler;

import cube.common.entity.AIGCConversationResponse;
import cube.dispatcher.aigc.AccessController;
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
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getRequestPath(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            String channelCode = null;
            String pattern = "chat";
            String content = null;
            JSONArray records = null;
            float temperature = -1;
            float topP = -1;
            float repetitionPenalty = -1;
            try {
                JSONObject json = this.readBodyAsJSONObject(request);
                channelCode = json.getString("code");
                content = json.getString("content");
                if (json.has("records")) {
                    records = json.getJSONArray("records");
                }
                if (json.has("temperature")) {
                    temperature = json.getFloat("temperature");
                }
                if (json.has("topP")) {
                    topP = json.getFloat("topP");
                }
                if (json.has("repetitionPenalty")) {
                    repetitionPenalty = json.getFloat("repetitionPenalty");
                }

                if (json.has("pattern")) {
                    pattern = json.getString("pattern");
                }
            } catch (Exception e) {
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
            AIGCConversationResponse convResponse = Manager.getInstance().conversation(token,
                    channelCode, pattern, content, records, temperature, topP, repetitionPenalty);
            if (null == convResponse) {
                // 发生错误
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
                return;
            }

            // Response
            JSONObject responseData = new JSONObject();
            responseData.put("participant", AI_NAME);
            responseData.put("sn", convResponse.sn);
            responseData.put("timestamp", System.currentTimeMillis());
            responseData.put("response", convResponse.toJSON());

            this.respondOk(response, responseData);
            this.complete();
        }
    }
}
