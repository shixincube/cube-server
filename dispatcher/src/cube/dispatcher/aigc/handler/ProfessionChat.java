/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

import cube.common.entity.AIGCChatRecord;
import cube.dispatcher.aigc.AccessController;
import cube.dispatcher.aigc.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ProfessionChat extends ContextHandler {

    private final static String AI_NAME = "Baize";

    public ProfessionChat() {
        super("/aigc/chat/profession");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        private AccessController controller;

        public Handler() {
            super();
            this.controller = new AccessController();
            this.controller.setEachIPInterval(1000);
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            if (!this.controller.filter(request)) {
                this.respond(response, HttpStatus.NOT_ACCEPTABLE_406);
                this.complete();
                return;
            }

            String token = this.getRequestPath(request);
            if (null == token || !token.equals(Manager.getInstance().getToken())) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            String channelCode = null;
            String content = null;
            String profession = null;
            try {
                JSONObject json = this.readBodyAsJSONObject(request);
                channelCode = json.getString("code");
                content = json.getString("content");
                profession = json.getString("profession");
            } catch (Exception e) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            if (null == channelCode || null == content || null == profession) {
                // 参数错误
                this.respond(response, HttpStatus.NOT_FOUND_404);
                this.complete();
                return;
            }

            // Chat
            AIGCChatRecord record = Manager.getInstance().chat(channelCode, content, profession);
            if (null == record) {
                // 发生错误
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
                return;
            }

            // Record 转结果
            JSONObject responseData = new JSONObject();
            responseData.put("participant", AI_NAME);
            responseData.put("content", record.answer);
            responseData.put("timestamp", record.timestamp);

            this.respondOk(response, responseData);
            this.complete();
        }
    }
}
