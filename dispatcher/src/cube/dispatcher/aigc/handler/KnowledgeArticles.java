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
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 知识库文章操作。
 */
public class KnowledgeArticles extends ContextHandler {

    public KnowledgeArticles() {
        super("/aigc/knowledge/article/");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        public Handler() {
            super();
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            // 获取文章数据
            String token = this.getApiToken(request);
            if (!Manager.getInstance().checkToken(token, this.getDevice(request))) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                this.complete();
                return;
            }

            JSONObject data = null;

            long startTime = 0;
            long endTime = 0;
            int activatedState = -1;
            long articleId = 0;
            String baseName = "document";
            try {
                String strArticleId = request.getParameter("id");
                if (null != strArticleId) {
                    articleId = Long.parseLong(strArticleId);

                    data = Manager.getInstance().getKnowledgeArticle(token, articleId);
                }
                else {
                    if (null != request.getParameter("base")) {
                        baseName = request.getParameter("base").trim();
                    }
                    startTime = Long.parseLong(request.getParameter("start"));
                    String end = request.getParameter("end");
                    if (null != end) {
                        endTime = Long.parseLong(end);
                    }
                    else {
                        endTime = System.currentTimeMillis();
                    }

                    if (null != request.getParameter("activated")) {
                        activatedState = Boolean.parseBoolean(request.getParameter("activated")) ? 1 : 0;
                    }

                    data = Manager.getInstance().getKnowledgeArticles(token, baseName, startTime, endTime, activatedState);
                }
            } catch (Exception e) {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
                return;
            }

            if (null == data) {
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
                return;
            }

            this.respondOk(response, data);
            this.complete();
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            // 更新文章数据
            String token = this.getApiToken(request);
            if (!Manager.getInstance().checkToken(token, this.getDevice(request))) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                this.complete();
                return;
            }

            KnowledgeArticle article = null;
            try {
                JSONObject data = readBodyAsJSONObject(request);
                if (null == data) {
                    this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                    this.complete();
                    return;
                }

                if (!data.has("contactId")) {
                    Manager.ContactToken contactToken = Manager.getInstance().getContactToken(token, this.getDevice(request));
                    data.put("contactId", contactToken.contact.getId());
                }

                article = new KnowledgeArticle(data);
            } catch (Exception e) {
                Logger.e(this.getClass(), "#doPost", e);
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
                return;
            }

            KnowledgeArticle result = Manager.getInstance().updateKnowledgeArticle(token, article);
            if (null == result) {
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
                return;
            }

            this.respondOk(response, result.toJSON());
            this.complete();
        }
    }
}
