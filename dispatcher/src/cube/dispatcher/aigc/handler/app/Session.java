/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler.app;

import cell.util.log.Logger;
import cube.aigc.AppEvent;
import cube.aigc.app.ConfigInfo;
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

    public final static String[] MODELS = new String[]{
            "Baize", "BaizeX", "BaizeNext",
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
            String version = request.getHeader("x-baize-api-version");

            if (null != version) {
                try {
                    String token = this.getApiToken(request);
                    if (null == token) {
                        this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                        this.complete();
                        return;
                    }

                    JSONObject data = this.readBodyAsJSONObject(request);
                    JSONObject eventData = new JSONObject();
                    eventData.put("token", token);
                    eventData.put("ip", data.has("ip") ? data.getString("ip") : request.getRemoteAddr());
                    eventData.put("device", data.has("device") ? data.getString("device") : "");
                    eventData.put("platform", data.has("platform") ? data.getString("platform") : "");
                    AppEvent appEvent = new AppEvent(AppEvent.Session, System.currentTimeMillis(), eventData);
                    if (!Manager.getInstance().addAppEvent(token, appEvent)) {
                        Logger.w(Session.class, "Write app event failed: " + token);
                    }

                    ConfigInfo configInfo = Manager.getInstance().getConfigInfo(token);
                    this.respondOk(response, configInfo.toJSON());
                    this.complete();
                } catch (Exception e) {
                    Logger.w(Session.class, "#doPost", e);
                    this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                    this.complete();
                }
            }
            else {
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

                        // 选择默认
                        ModelConfig modelConfig = null;
                        for (String modelName : MODELS) {
                            modelConfig = Manager.getInstance().getModelConfigByModel(configInfo, modelName);
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
                            KnowledgeProfile profile = Manager.getInstance().getKnowledgeProfile(token);
                            if (null != profile) {
                                responseData.put("knowledge", profile.toJSON());
                            }
                            else {
                                responseData.put("knowledge", KnowledgeProfile.createDummy().toJSON());
                                Logger.w(this.getClass(), "#doPost - Knowledge profile is null - token: " + token);
                            }

                            // 知识库框架，仅为了兼容旧版本
                            responseData.put("knowledgeFramework", Manager.getInstance().getKnowledgeFramework(token));

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
                        responseData.put("knowledge", KnowledgeProfile.createDummy().toJSON());
                        Logger.w(Session.class, "Manager check token failed: " + token);
                    }
                }
                else {
                    responseData.put("auth", false);
                    responseData.put("knowledge", KnowledgeProfile.createDummy().toJSON());
                    Logger.w(Session.class, "Token is null");
                }

                Helper.respondOk(this, response, responseData);
            }
        }
    }
}
