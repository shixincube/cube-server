/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cube.dispatcher.aigc.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 说话者分隔与分析。
 */
public class AudioDiarizationOperation extends ContextHandler {

    public AudioDiarizationOperation() {
        super("/aigc/audio/diarization/opt/");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        public Handler() {
            super();
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getApiToken(request);
            if (!Manager.getInstance().checkToken(token, this.getDevice(request))) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                this.complete();
                return;
            }

            String pathInfo = request.getPathInfo();
            if (pathInfo.startsWith("/list")) {
                this.optList(token, request, response);
            }
            else {
                this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                this.complete();
            }
        }

        private void optList(String token, HttpServletRequest request, HttpServletResponse response) {
            JSONObject result = Manager.getInstance().listSpeakerDiarizations(token);
            if (null == result) {
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
                return;
            }

            this.respondOk(response, result);
            this.complete();
        }
    }
}
