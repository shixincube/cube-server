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

package cube.dispatcher.aigc.handler.app;

import cell.util.log.Logger;
import cube.aigc.AppEvent;
import cube.aigc.ConfigInfo;
import cube.aigc.ModelConfig;
import cube.common.entity.KnowledgeProfile;
import cube.dispatcher.aigc.Manager;
import cube.dispatcher.aigc.handler.AIGCHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Session extends ContextHandler {

    public final static String[] MODEL_NAMES = new String[]{
            "Wenxin", "GPT", "Gemini", "DallE", "Baize"
    };

    public Session() {
        super("/app/session/");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        public Handler() {
            super();
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            String token = null;
            String ip = null;
            try {
                JSONObject data = this.readBodyAsJSONObject(request);
                if (null == data) {
                    this.respond(response, HttpStatus.BAD_REQUEST_400);
                    this.complete();
                    return;
                }

                if (!data.has("app")) {
                    this.respond(response, HttpStatus.BAD_REQUEST_400);
                    this.complete();
                    return;
                }

                if (!data.getString("app").equalsIgnoreCase("baize")) {
                    this.respond(response, HttpStatus.BAD_REQUEST_400);
                    this.complete();
                    return;
                }

                if (data.has("token")) {
                    token = data.getString("token");
                }

                if (data.has("ip")) {
                    ip = data.getString("ip");
                }
            } catch (Exception e) {
                Logger.w(Session.class, "#doPost", e);
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            JSONObject responseData = new JSONObject();

            if (null == token || token.length() == 0) {
                token = null;
                String authorization = request.getHeader("Authorization");
                if (null == authorization) {
                    Logger.w(Session.class, "No authorization data");
                }
                else {
                    String[] tmp = authorization.split(" ");
                    if (tmp.length > 1) {
                        token = tmp[1].trim();
                    }
                }
            }

            if (null != token) {
                if (Manager.getInstance().checkToken(token)) {
                    JSONObject eventData = new JSONObject();
                    eventData.put("token", token);
                    if (null != ip) {
                        eventData.put("ip", ip);
                    }

                    // 获取模型配置
                    ConfigInfo configInfo = Manager.getInstance().getConfigInfo(token);
                    ModelConfig modelConfig = null;
                    for (String modelName : MODEL_NAMES) {
                        modelConfig = Manager.getInstance().getModelConfig(configInfo, modelName);
                        if (null != modelConfig) {
                            break;
                        }
                    }
                    responseData.put("selectedModel", modelConfig.toJSON());

                    // 申请频道
                    App.ChannelInfo channel = App.getInstance().openChannel(token, modelConfig);
                    if (null != channel) {
                        responseData.put("auth", true);
                        responseData.put("channel", channel.code);
                        responseData.put("models", configInfo.getModelsAsJSONArray());

                        Manager.ContactToken contactToken = Manager.getInstance().getContactToken(token);
                        responseData.put("context", contactToken.toJSON());

                        // 知识库概述
                        responseData.put("knowledge", Manager.getInstance().getKnowledgeProfile(token).toJSON());

                        eventData.put("auth", true);
                    }
                    else {
                        responseData.put("auth", false);
                        responseData.put("knowledge", KnowledgeProfile.createDummy().toJSON());
                        Logger.w(Session.class, "Failed to open channel for token: " + token);

                        eventData.put("auth", false);
                    }

                    // 记录事件
                    AppEvent appEvent = new AppEvent(AppEvent.Session, System.currentTimeMillis(), eventData);
                    if (!Manager.getInstance().addAppEvent(token, appEvent)) {
                        Logger.w(Session.class, "Write app event failed: " + token);
                    }
                }
                else {
                    responseData.put("auth", false);
                    responseData.put("knowledge", KnowledgeProfile.createDummy());
                    Logger.w(Session.class, "Manager check token failed: " + token);
                }
            }
            else {
                responseData.put("auth", false);
                responseData.put("knowledge", KnowledgeProfile.createDummy());
                Logger.w(Session.class, "Token is null");
            }

            Helper.respondOk(this, response, responseData);
        }
    }
}
