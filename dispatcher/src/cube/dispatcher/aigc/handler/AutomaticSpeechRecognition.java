/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cube.auth.AuthConsts;
import cube.dispatcher.aigc.AccessController;
import cube.dispatcher.aigc.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 自动语音识别。
 */
public class AutomaticSpeechRecognition extends ContextHandler {

    public AutomaticSpeechRecognition() {
        super("/aigc/audio/asr");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        private AccessController controller;

        public Handler() {
            super();
            this.controller = new AccessController();
            this.controller.setEachIPInterval(200);
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            if (!this.controller.filter(request)) {
                this.respond(response, HttpStatus.NOT_ACCEPTABLE_406);
                this.complete();
                return;
            }

            String token = this.getRequestPath(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            String fileCode = request.getParameter("fc");
            if (null == fileCode) {
                fileCode = request.getParameter("code");
            }

            if (null == fileCode) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            Manager.ASRFuture future = Manager.getInstance().getASRFuture(fileCode);
            if (null == future) {
                this.respond(response, HttpStatus.NOT_FOUND_404);
                this.complete();
                return;
            }

            this.respondOk(response, future.toJSON());
            this.complete();
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            if (!this.controller.filter(request)) {
                this.respond(response, HttpStatus.NOT_ACCEPTABLE_406);
                this.complete();
                return;
            }

            String token = this.getRequestPath(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            String domain = AuthConsts.DEFAULT_DOMAIN;
            String fileCode = null;
            try {
                JSONObject json = this.readBodyAsJSONObject(request);
                fileCode = json.getString("fileCode");
                if (json.has("domain")) {
                    domain = json.getString("domain");
                }
            } catch (Exception e) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            if (null == fileCode) {
                // 参数错误
                this.respond(response, HttpStatus.NOT_FOUND_404);
                this.complete();
                return;
            }

            // ASR
            Manager.ASRFuture future = Manager.getInstance().automaticSpeechRecognition(domain, fileCode);
            if (null == future) {
                // 故障
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
                return;
            }

            this.respondOk(response, future.toJSON());
            this.complete();
        }
    }
}
