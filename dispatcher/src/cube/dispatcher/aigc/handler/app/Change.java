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
import cube.aigc.ConfigInfo;
import cube.aigc.ModelConfig;
import cube.dispatcher.aigc.Manager;
import cube.dispatcher.aigc.handler.AIGCHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Change extends ContextHandler {

    public Change() {
        super("/app/change/");
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

            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            // 当前使用的模型
            ModelConfig modelConfig = App.getInstance().getModelConfig(token);
            if (null == modelConfig) {
                this.respond(response, HttpStatus.NOT_FOUND_404);
                this.complete();
                return;
            }

            ModelConfig newModelConfig = null;
            ConfigInfo configInfo = Manager.getInstance().getConfigInfo(token);

            try {
                JSONObject data = this.readBodyAsJSONObject(request);
                if (null == data) {
                    this.respond(response, HttpStatus.BAD_REQUEST_400);
                    this.complete();
                    return;
                }

                String model = data.getString("model");

                if (modelConfig.getName().equals(model)) {
                    // 模型一致，返回成功
                    App.ChannelInfo channelInfo = App.getInstance().getChannel(token);
                    Helper.respondOk(this, response, channelInfo.toJSON());
                    return;
                }

                newModelConfig = Manager.getInstance().getModelConfig(configInfo, model);
            } catch (Exception e) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
                return;
            }

            if (null == newModelConfig) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
                return;
            }

            App.ChannelInfo newChannelInfo = App.getInstance().resetModel(token, newModelConfig);

            JSONObject responseData = new JSONObject();
            responseData.put("auth", true);
            responseData.put("channel", newChannelInfo.code);
            responseData.put("selectedModel", newModelConfig.toJSON());
            responseData.put("models", configInfo.getModelsAsJSONArray());

            Manager.ContactToken contactToken = Manager.getInstance().getContactToken(token);
            responseData.put("context", contactToken.toJSON());

            // 知识库概述
            responseData.put("knowledge", Manager.getInstance().getKnowledgeProfile(token).toJSON());

            // 知识库框架
            responseData.put("knowledgeFramework", Manager.getInstance().getKnowledgeFramework(token));

            Helper.respondOk(this, response, responseData);

            Logger.d(Change.class, "Client reset session for changing model: " + token);
        }
    }
}
