/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler.app;

import cell.util.log.Logger;
import cube.common.action.AIGCAction;
import cube.dispatcher.aigc.Manager;
import cube.dispatcher.aigc.handler.AIGCHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Schedules extends ContextHandler {

    public Schedules() {
        super("/app/schedule/");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        public Handler() {
            super();
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getApiToken(request);
            if (null == token) {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
                return;
            }

            if (!Manager.getInstance().checkToken(token, this.getDevice(request))) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                this.complete();
                return;
            }

            try {
                if (null == request.getParameter("starting") || null == request.getParameter("ending")) {
                    this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                    this.complete();
                    return;
                }

                long starting = Long.parseLong(request.getParameter("starting"));
                long ending = Long.parseLong(request.getParameter("ending"));

                JSONObject requestData = new JSONObject();
                requestData.put("starting", starting);
                requestData.put("ending", ending);
                JSONObject responseData = Manager.getInstance().syncRequest(token, AIGCAction.AppQuerySchedule, requestData);
                if (null == responseData) {
                    this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                    this.complete();
                    return;
                }

                this.respondOk(response, responseData);
                this.complete();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#doGet", e);
                this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                this.complete();
            }
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getApiToken(request);
            if (null == token) {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
                return;
            }

            if (!Manager.getInstance().checkToken(token, this.getDevice(request))) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                this.complete();
                return;
            }

            try {
                JSONObject requestData = this.readBodyAsJSONObject(request);
                JSONObject responseData = Manager.getInstance().syncRequest(token, AIGCAction.AppUpdateSchedule, requestData);
                if (null == responseData) {
                    this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                    this.complete();
                    return;
                }

                this.respondOk(response, responseData);
                this.complete();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#doPost", e);
                this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                this.complete();
            }
        }
    }
}
