/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cell.util.log.Logger;
import cube.aigc.TextSplitter;
import cube.common.entity.KnowledgeDocument;
import cube.common.entity.KnowledgeProgress;
import cube.dispatcher.aigc.AccessController;
import cube.dispatcher.aigc.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 导入知识库文档。
 */
public class ImportKnowledgeDoc extends ContextHandler {

    public ImportKnowledgeDoc() {
        super("/aigc/knowledge/import/");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        private AccessController controller;

        public Handler() {
            super();
            this.controller = new AccessController();
            this.controller.setEachIPInterval(100);
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            if (!this.controller.filter(request)) {
                this.respond(response, HttpStatus.NOT_ACCEPTABLE_406, this.makeError(HttpStatus.NOT_ACCEPTABLE_406));
                this.complete();
                return;
            }

            String token = this.getApiToken(request);
            if (!Manager.getInstance().checkToken(token, this.getDevice(request))) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                this.complete();
                return;
            }

            try {
                JSONObject data = readBodyAsJSONObject(request);
                if (null == data) {
                    this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                    this.complete();
                    return;
                }

                String baseName = data.has("base") ? data.getString("base") : "document";
                if (null == baseName) {
                    this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                    this.complete();
                    return;
                }

                TextSplitter splitter = TextSplitter.Auto;
                if (data.has("splitter")) {
                    splitter = TextSplitter.parse(data.getString("splitter"));
                }

                if (data.has("fileCode")) {
                    String fileCode = data.getString("fileCode");
                    KnowledgeDocument doc = Manager.getInstance().importKnowledgeDoc(token, baseName, fileCode, splitter);
                    if (null == doc) {
                        this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                        this.complete();
                    }
                    else {
                        this.respondOk(response, doc.toJSON());
                        this.complete();
                    }
                }
                else if (data.has("fileCodeList")) {
                    JSONArray fileCodeList = data.getJSONArray("fileCodeList");
                    KnowledgeProgress progress = Manager.getInstance().importKnowledgeDocs(token, baseName, fileCodeList, splitter);
                    if (null == progress) {
                        this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                        this.complete();
                    }
                    else {
                        this.respondOk(response, progress.toJSON());
                        this.complete();
                    }
                }
                else {
                    this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                    this.complete();
                }
            } catch (Exception e) {
                Logger.w(this.getClass(), "#doPost", e);
                this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
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

            try {
                String paramSN = request.getParameter("sn");
                String baseName =request.getParameter("base");
                KnowledgeProgress progress = Manager.getInstance().getKnowledgeProgress(token, baseName, Long.parseLong(paramSN));
                if (null == progress) {
                    this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                    this.complete();
                    return;
                }

                this.respondOk(response, progress.toJSON());
                this.complete();
            } catch (Exception e) {
                this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                this.complete();
            }
        }
    }
}
