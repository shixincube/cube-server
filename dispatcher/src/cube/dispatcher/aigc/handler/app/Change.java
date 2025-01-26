/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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

                // 2024-1-20 更新为 name 参数，参数 model 向下兼容
                String name = data.has("name") ? data.getString("name") : data.getString("model");

                if (modelConfig.getName().equals(name) || modelConfig.getModel().equals(name)) {
                    // 模型一致，返回成功
                    App.ChannelInfo channelInfo = App.getInstance().getChannel(token);
                    Helper.respondOk(this, response, channelInfo.toJSON());
                    return;
                }

                newModelConfig = Manager.getInstance().getModelConfigByName(configInfo, name);
                if (null == newModelConfig) {
                    newModelConfig = Manager.getInstance().getModelConfigByModel(configInfo, name);
                }
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

            // 知识库框架，仅为了兼容旧版本
            responseData.put("knowledgeFramework", Manager.getInstance().getKnowledgeFramework(token));

            Helper.respondOk(this, response, responseData);

            Logger.d(Change.class, "Client reset session for changing model: " + token);
        }
    }
}
