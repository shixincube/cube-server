/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler.app;

import cell.util.log.Logger;
import cube.aigc.psychology.app.AppUserProfile;
import cube.dispatcher.aigc.Manager;
import cube.dispatcher.aigc.handler.AIGCHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 用户侧写数据。
 */
public class UserProfile extends ContextHandler {

    public UserProfile() {
        super("/app/user/profile/");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        public Handler() {
            super();
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            try {
                String token = getApiToken(request);
                if (!Manager.getInstance().checkToken(token)) {
                    this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                    this.complete();
                    return;
                }

                AppUserProfile appUserProfile = Manager.getInstance().getUserProfile(token);
                if (null == appUserProfile) {
                    this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                    this.complete();
                    return;
                }

                this.respondOk(response, appUserProfile.toJSON());
                this.complete();
            } catch (Exception e) {
                Logger.w(this.getClass(), "#doGet", e);
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
            }
        }
    }
}
