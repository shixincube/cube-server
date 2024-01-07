/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Cube Team.
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

            String unit = ModelConfig.BAIZE_UNIT;
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
                    String sectionQuery = data.getString("sectionQuery");
                    String comprehensiveQuery = data.getString("comprehensiveQuery");
                    String category = data.getString("category");
                    matchingSchema = new KnowledgeMatchingSchema(category, sectionQuery, comprehensiveQuery);
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
