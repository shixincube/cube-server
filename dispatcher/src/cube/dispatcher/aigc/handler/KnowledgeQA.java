/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cube.common.entity.KnowledgeQAProgress;
import cube.dispatcher.aigc.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * 基于知识库的问答。
 */
public class KnowledgeQA extends ContextHandler {

    public KnowledgeQA() {
        super("/aigc/knowledge/qa/");
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

            try {
                JSONObject data = this.readBodyAsJSONObject(request);
                String channel = data.getString("channel");
                String query = data.getString("query");
                boolean sync = data.has("sync") ? data.getBoolean("sync") : true;
                String base = data.has("base") ? data.getString("base") : null;

                KnowledgeQAProgress result = Manager.getInstance().performKnowledgeQA(token, channel,
                        query, base, sync);
                if (null == result) {
                    this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                    this.complete();
                    return;
                }

                this.respondOk(response, result.toJSON());
                this.complete();
            } catch (Exception e) {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
            }
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getApiToken(request);
            if (!Manager.getInstance().checkToken(token, this.getDevice(request))) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                this.complete();
                return;
            }

            String channel = request.getParameter("channel");
            if (null == channel) {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
                return;
            }

            String base = request.getParameter("base");
            if (null != base) {
                try {
                    base = URLDecoder.decode(base, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            KnowledgeQAProgress progress = Manager.getInstance().getKnowledgeQAProgress(token, channel, base);
            if (null == progress) {
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
                return;
            }

            this.respondOk(response, progress.toJSON());
            this.complete();
        }
    }
}
