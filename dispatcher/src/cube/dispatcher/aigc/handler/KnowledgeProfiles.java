/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cube.common.entity.KnowledgeProfile;
import cube.common.entity.KnowledgeScope;
import cube.dispatcher.aigc.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 知识库配置信息。
 */
public class KnowledgeProfiles extends ContextHandler {

    public KnowledgeProfiles() {
        super("/aigc/knowledge/profile/");
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

            long contactId = 0;
            int state = -1;
            long maxSize = -1;
            KnowledgeScope scope = null;

            try {
                JSONObject json = this.readBodyAsJSONObject(request);
                if (json.has("contactId")) {
                    contactId = json.getLong("contactId");
                }
                if (json.has("state")) {
                    state = json.getInt("state");
                }
                if (json.has("maxSize")) {
                    maxSize = json.getLong("maxSize");
                }
                if (json.has("scope")) {
                    scope = KnowledgeScope.parse(json.getString("scope"));
                }
            } catch (Exception e) {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
                return;
            }

            KnowledgeProfile profile = Manager.getInstance().updateKnowledgeProfile(token, contactId,
                    state, maxSize, scope);
            if (null == profile) {
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
                return;
            }

            this.respondOk(response, profile.toJSON());
            this.complete();
        }
    }
}
