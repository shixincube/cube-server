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
 * 生成文本摘要。
 */
public class Summarization extends ContextHandler {

    public Summarization() {
        super("/aigc/nlp/summarization");
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

            String text = null;
            try {
                JSONObject json = this.readBodyAsJSONObject(request);
                text = json.getString("text");
            } catch (Exception e) {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
                return;
            }

            if (null == text) {
                // 参数错误
                this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                this.complete();
                return;
            }

            // 文本摘要
            String result = Manager.getInstance().generateSummarization(text);
            if (null == result) {
                // 不允许该参与者申请或者服务故障
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
                return;
            }

            JSONObject responseData = new JSONObject();
            responseData.put("summarization", result);
            this.respondOk(response, responseData);
            this.complete();
        }
    }
}
