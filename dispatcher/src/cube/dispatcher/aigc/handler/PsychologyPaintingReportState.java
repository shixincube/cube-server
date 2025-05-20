/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cube.dispatcher.aigc.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 心理学绘画报告状态操作。
 */
public class PsychologyPaintingReportState extends ContextHandler {

    public PsychologyPaintingReportState() {
        super("/aigc/psychology/report/state");
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

            try {
                JSONObject data = this.readBodyAsJSONObject(request);

                long sn = data.getLong("sn");
                int state = data.getInt("state");

                if (Manager.getInstance().setPaintingReportState(token, sn, state)) {
                    this.respondOk(response, data);
                }
                else {
                    this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                }

                this.complete();
            } catch (Exception e) {
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
            }
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getApiToken(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                this.complete();
                return;
            }

            try {
                long sn = Long.parseLong(request.getParameter("sn"));

                JSONObject result = Manager.getInstance().getPsychologyReportPart(token, sn,
                        false, false, false, false, false, false);
                if (null != result) {
                    this.respondOk(response, result);
                }
                else {
                    this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                }
                this.complete();
            } catch (Exception e) {
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
            }
        }
    }
}
