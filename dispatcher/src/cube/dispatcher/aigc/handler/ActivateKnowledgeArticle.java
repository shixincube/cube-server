/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cell.util.log.Logger;
import cube.common.entity.KnowledgeArticle;
import cube.dispatcher.aigc.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 激活知识库文章。
 */
public class ActivateKnowledgeArticle extends ContextHandler {

    public ActivateKnowledgeArticle() {
        super("/aigc/knowledge/article/activate");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        public Handler() {
            super();
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getApiToken(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                this.complete();
                return;
            }

            String base = null;
            JSONArray idList = null;
            try {
                JSONObject data = readBodyAsJSONObject(request);
                if (null == data) {
                    this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                    this.complete();
                    return;
                }

                base = data.getString("base");
                idList = data.getJSONArray("ids");
            } catch (Exception e) {
                Logger.e(this.getClass(), "#doPost", e);
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
                return;
            }

            List<KnowledgeArticle> result = Manager.getInstance().activateKnowledgeArticle(token, base, idList);
            if (null == result) {
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
                return;
            }

            JSONArray list = new JSONArray();
            for (KnowledgeArticle article : result) {
                list.put(article.toCompactJSON());
            }

            JSONObject responsePayload = new JSONObject();
            responsePayload.put("total", list.length());
            responsePayload.put("list", list);
            this.respondOk(response, responsePayload);
            this.complete();
        }
    }
}
