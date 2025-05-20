/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cell.util.log.Logger;
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
            String token = this.getApiToken(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                this.complete();
                return;
            }

            try {
                long sn = Long.parseLong(request.getParameter("sn"));
                boolean content = null != request.getParameter("content") &&
                        Boolean.parseBoolean(request.getParameter("content"));
                boolean section = null != request.getParameter("section") &&
                        Boolean.parseBoolean(request.getParameter("section"));
                boolean thought = null != request.getParameter("thought") &&
                        Boolean.parseBoolean(request.getParameter("thought"));
                boolean summary = null != request.getParameter("summary") &&
                        Boolean.parseBoolean(request.getParameter("summary"));
                boolean rating = null != request.getParameter("rating") &&
                        Boolean.parseBoolean(request.getParameter("rating"));
                boolean link = null != request.getParameter("link") &&
                        Boolean.parseBoolean(request.getParameter("link"));

                JSONObject data = Manager.getInstance().getPsychologyReportPart(token, sn, content, section, thought,
                        summary, rating, link);
                if (null != data) {
                    this.respondOk(response, data);
                }
                else {
                    this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                }
                this.complete();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#doGet", e);
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
            }
        }
    }
}
