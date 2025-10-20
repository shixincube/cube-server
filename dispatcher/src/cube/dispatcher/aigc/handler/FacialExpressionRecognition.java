/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cube.dispatcher.aigc.Manager;
import cube.util.FileLabels;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 面部表情识别。
 */
public class FacialExpressionRecognition extends ContextHandler {

    public FacialExpressionRecognition() {
        super("/aigc/facial/expression");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        public Handler() {
            super();
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getApiToken(request);
            if (!Manager.getInstance().checkToken(token, this.getDevice(request))) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                this.complete();
                return;
            }

            String fileCode = request.getParameter("fc");
            if (null == fileCode) {
                fileCode = request.getParameter("code");
            }

            if (null == fileCode) {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
                return;
            }

            Manager.FacialExpressionRecognitionFuture future = Manager.getInstance().getFacialExpressionRecognitionFuture(fileCode);
            if (null == future) {
                this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                this.complete();
                return;
            }

            JSONObject responseData = future.toJSON();
            if (responseData.has("result")) {
                JSONObject fileJson = responseData.getJSONObject("result").getJSONObject("file");
                FileLabels.reviseFileLabel(fileJson, token,
                        Manager.getInstance().getPerformer().getExternalHttpEndpoint(),
                        Manager.getInstance().getPerformer().getExternalHttpsEndpoint());

                if (responseData.getJSONObject("result").has("visualization")) {
                    JSONObject visualization = responseData.getJSONObject("result").getJSONObject("visualization");
                    FileLabels.reviseFileLabel(visualization, token,
                            Manager.getInstance().getPerformer().getExternalHttpEndpoint(),
                            Manager.getInstance().getPerformer().getExternalHttpsEndpoint());
                }
            }

            this.respondOk(response, responseData);
            this.complete();
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getApiToken(request);
            if (!Manager.getInstance().checkToken(token, this.getDevice(request))) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                this.complete();
                return;
            }

            try {
                JSONObject json = this.readBodyAsJSONObject(request);
                String fileCode = json.getString("fileCode");
                boolean visualize = json.has("visualize") && json.getBoolean("visualize");

                boolean reset = json.has("reset") && json.getBoolean("reset");

                Manager.FacialExpressionRecognitionFuture future =
                        Manager.getInstance().facialExpressionRecognition(token, fileCode, visualize, reset);
                if (null == future) {
                    // 故障
                    this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                    this.complete();
                    return;
                }

                this.respondOk(response, future.toJSON());
                this.complete();
            } catch (Exception e) {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
            }
        }
    }
}
