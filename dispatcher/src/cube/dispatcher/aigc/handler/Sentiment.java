/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cube.common.entity.SentimentResult;
import cube.dispatcher.aigc.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 情感分析。
 * @deprecated
 */
public class Sentiment extends ContextHandler {

    public Sentiment() {
        super("/aigc/nlp/sentiment");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

//        private AccessController controller;

        public Handler() {
            super();
//            this.controller = new AccessController();
//            this.controller.setEachIPInterval(100);
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
//            if (!this.controller.filter(request)) {
//                this.respond(response, HttpStatus.NOT_ACCEPTABLE_406);
//                this.complete();
//                return;
//            }

            String token = this.getRequestPath(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            String text = null;
            try {
                JSONObject json = this.readBodyAsJSONObject(request);
                text = json.getString("text");
            } catch (Exception e) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            if (null == text) {
                // 参数错误
                this.respond(response, HttpStatus.NOT_FOUND_404);
                this.complete();
                return;
            }

            // 情感分析
            SentimentResult result = null;//Manager.getInstance().sentimentAnalysis(text);
            if (null == result) {
                // 不允许该参与者申请或者服务故障
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
                return;
            }

            this.respondOk(response, result.toJSON());
            this.complete();
        }
    }
}
