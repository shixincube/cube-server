/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cube.common.entity.ResetKnowledgeProgress;
import cube.dispatcher.aigc.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 重置知识库数据操作。
 */
public class ResetKnowledgeStore extends ContextHandler {

    public ResetKnowledgeStore() {
        super("/aigc/knowledge/reset/");
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

            String baseName = null;
            boolean backup = true;
            try {
                JSONObject data = this.readBodyAsJSONObject(request);
                baseName = data.getString("base");
                if (data.has("backup")) {
                    backup = data.getBoolean("backup");
                }
            } catch (Exception e) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            // 重置
            ResetKnowledgeProgress progress = Manager.getInstance().resetKnowledgeStore(token, baseName, backup);
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

            long sn = 0;
            String baseName = null;
            try {
                sn = Long.parseLong(request.getParameter("sn"));
                baseName = request.getParameter("base");
            } catch (Exception e) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            // 获取重置进度
            ResetKnowledgeProgress progress = Manager.getInstance().getResetKnowledgeProgress(token, sn, baseName);
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
