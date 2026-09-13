/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cell.util.log.Logger;
import cube.common.entity.MultimodalInput;
import cube.common.entity.MultimodalOutput;
import cube.dispatcher.aigc.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 多模态调用。
 */
public class MultimodalBase extends ContextHandler {

    public MultimodalBase() {
        super("/multimodal/base/");
        setHandler(new Handler());
    }

    private static class Handler extends AIGCHandler {

        public Handler() {
            super();
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getApiToken(request);
            if (!Manager.getInstance().checkToken(token, this.getDevice(request))) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            String channelCode;
            MultimodalInput multimodalInput;
            try {
                JSONObject json = this.readBodyAsJSONObject(request);
                channelCode = json.getString("channel");
                multimodalInput = new MultimodalInput(json.getJSONObject("input"));
            } catch (Exception e) {
                Logger.e(Chat.class, "#doPost - Read body failed", e);
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            // Multimodal
            MultimodalOutput result = Manager.getInstance().executeMultimodal(token, channelCode, multimodalInput);
            if (null == result) {
                // 发生错误
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
                return;
            }

            // Response
            JSONObject responseData = result.toCompactJSON();
            this.respondOk(response, responseData);
            this.complete();
        }
    }
}
