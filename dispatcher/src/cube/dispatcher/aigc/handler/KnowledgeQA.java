/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cube.aigc.ModelConfig;
import cube.common.entity.KnowledgeMatchingSchema;
import cube.common.entity.KnowledgeQAProgress;
import cube.dispatcher.aigc.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
            String token = this.getLastRequestPath(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            String unit = ModelConfig.CHAT_UNIT;
            KnowledgeMatchingSchema matchingSchema = null;
            try {
                JSONObject data = this.readBodyAsJSONObject(request);
                if (data.has("unit")) {
                    unit = data.getString("unit");
                }

                if (data.has("matchingSchema")) {
                    matchingSchema = new KnowledgeMatchingSchema(data.getJSONObject("matchingSchema"));
                }
                else {
//                    String sectionQuery = data.getString("sectionQuery");
                    String comprehensiveQuery = data.getString("comprehensiveQuery");
                    String category = data.getString("category");
                    matchingSchema = new KnowledgeMatchingSchema(category, comprehensiveQuery);
                }
            } catch (Exception e) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            KnowledgeQAProgress progress = Manager.getInstance().performKnowledgeQA(token, unit, matchingSchema);
            if (null == progress) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
                return;
            }

            this.respondOk(response, progress.toJSON());
            this.complete();
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getLastRequestPath(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            KnowledgeQAProgress progress = Manager.getInstance().getKnowledgeQAProgress(token);
            if (null == progress) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
                return;
            }

            this.respondOk(response, progress.toJSON());
            this.complete();
        }
    }
}
