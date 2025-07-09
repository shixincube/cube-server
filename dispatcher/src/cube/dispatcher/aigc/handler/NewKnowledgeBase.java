/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cube.common.entity.KnowledgeBaseInfo;
import cube.common.entity.KnowledgeScope;
import cube.dispatcher.aigc.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 创建知识库。
 */
public class NewKnowledgeBase extends ContextHandler {

    public NewKnowledgeBase() {
        super("/aigc/knowledge/new/");
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

            String baseName = null;
            String displayName = null;
            String category = null;
            KnowledgeScope scope = KnowledgeScope.Private;
            try {
                JSONObject data = this.readBodyAsJSONObject(request);
                baseName = data.getString("name");
                displayName = data.getString("displayName");
                if (data.has("category")) {
                    category = data.getString("category");
                }
                else {
                    category = displayName;
                }
                if (data.has("scope")) {
                    scope = KnowledgeScope.parse(data.getString("scope"));
                }
            } catch (Exception e) {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
                return;
            }

            KnowledgeBaseInfo info = Manager.getInstance().newKnowledgeBase(token, baseName, displayName, category, scope);
            if (null == info) {
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
                return;
            }

            this.respondOk(response, info.toJSON());
            this.complete();
        }
    }
}
