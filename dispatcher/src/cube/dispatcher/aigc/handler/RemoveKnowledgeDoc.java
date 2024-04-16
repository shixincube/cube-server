/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.dispatcher.aigc.handler;

import cube.common.entity.KnowledgeDoc;
import cube.common.entity.KnowledgeProgress;
import cube.dispatcher.aigc.AccessController;
import cube.dispatcher.aigc.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 移除知识库文档。
 */
public class RemoveKnowledgeDoc extends ContextHandler {

    public RemoveKnowledgeDoc() {
        super("/aigc/knowledge/remove/");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        private AccessController controller;

        public Handler() {
            super();
            this.controller = new AccessController();
            this.controller.setEachIPInterval(1000);
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            if (!this.controller.filter(request)) {
                this.respond(response, HttpStatus.NOT_ACCEPTABLE_406);
                this.complete();
                return;
            }

            String token = this.getRequestPath(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            try {
                JSONObject data = readBodyAsJSONObject(request);
                if (null == data) {
                    this.respond(response, HttpStatus.FORBIDDEN_403);
                    this.complete();
                    return;
                }

                String baseName = data.getString("base");
                if (null == baseName) {
                    this.respond(response, HttpStatus.FORBIDDEN_403);
                    this.complete();
                    return;
                }

                if (data.has("fileCode")) {
                    String fileCode = data.getString("fileCode");
                    KnowledgeDoc doc = Manager.getInstance().removeKnowledgeDoc(token, baseName, fileCode);
                    if (null == doc) {
                        this.respond(response, HttpStatus.BAD_REQUEST_400);
                        this.complete();
                    }
                    else {
                        this.respondOk(response, doc.toJSON());
                        this.complete();
                    }
                }
                else if (data.has("fileCodeList")) {
                    JSONArray fileCodeList = data.getJSONArray("fileCodeList");
                    KnowledgeProgress progress = Manager.getInstance().removeKnowledgeDocs(token, baseName, fileCodeList);
                    if (null == progress) {
                        this.respond(response, HttpStatus.BAD_REQUEST_400);
                        this.complete();
                    }
                    else {
                        this.respondOk(response, progress.toJSON());
                        this.complete();
                    }
                }
                else {
                    this.respond(response, HttpStatus.FORBIDDEN_403);
                    this.complete();
                }
            } catch (IOException e) {
                this.respond(response, HttpStatus.NOT_FOUND_404);
                this.complete();
            }
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getLastRequestPath(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            try {
                String paramSN = request.getParameter("sn");
                String baseName = request.getParameter("base");
                KnowledgeProgress progress = Manager.getInstance().getKnowledgeProgress(token, baseName, Long.parseLong(paramSN));
                if (null == progress) {
                    this.respond(response, HttpStatus.BAD_REQUEST_400);
                    this.complete();
                    return;
                }

                this.respondOk(response, progress.toJSON());
                this.complete();
            } catch (Exception e) {
                this.respond(response, HttpStatus.NOT_FOUND_404);
                this.complete();
            }
        }
    }
}
