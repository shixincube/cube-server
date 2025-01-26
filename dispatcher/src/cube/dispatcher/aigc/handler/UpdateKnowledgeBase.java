/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cube.common.entity.KnowledgeBaseInfo;
import cube.dispatcher.aigc.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 更新知识库信息。
 */
public class UpdateKnowledgeBase extends ContextHandler {

    public UpdateKnowledgeBase() {
        super("/aigc/knowledge/update/");
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

            KnowledgeBaseInfo info = null;
            try {
                JSONObject data = this.readBodyAsJSONObject(request);
                info = new KnowledgeBaseInfo(data.getJSONObject("info"));
            } catch (Exception e) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            KnowledgeBaseInfo result = Manager.getInstance().updateKnowledgeBase(token, info);
            if (null == result) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
                return;
            }

            this.respondOk(response, result.toJSON());
            this.complete();
        }
    }
}
