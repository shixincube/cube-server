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
 * 心理学报告部分数据。
 */
public class PsychologyReportParts extends ContextHandler {

    public PsychologyReportParts() {
        super("/aigc/psychology/report/part");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        public Handler() {
            super();
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
                long sn = Long.parseLong(request.getParameter("sn"));
                boolean reportText = null != request.getParameter("reports") &&
                        Boolean.parseBoolean(request.getParameter("reports"));

                JSONObject data = Manager.getInstance().getPsychologyReportPart(token, sn, reportText);
                if (null != data) {
                    this.respondOk(response, data);
                }
                else {
                    this.respond(response, HttpStatus.NOT_FOUND_404);
                }
                this.complete();
            } catch (Exception e) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
            }
        }
    }
}
