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

import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.PsychologyReport;
import cube.aigc.psychology.Theme;
import cube.dispatcher.aigc.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 心理学绘画预测。
 */
public class PsychologyReports extends ContextHandler {

    public PsychologyReports() {
        super("/aigc/psychology/report");
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

            try {
                JSONObject data = this.readBodyAsJSONObject(request);
                Attribute attribute = new Attribute(data.getJSONObject("attribute"));
                String fileCode = data.getString("fileCode");
                String theme = data.has("theme") ? data.getString("theme") : Theme.Generic.code;
                PsychologyReport report =
                        Manager.getInstance().generatePsychologyReport(token, attribute, fileCode, theme);
                if (null != report) {
                    this.respondOk(response, report.toJSON());
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

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getLastRequestPath(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            try {
                boolean markdown = null != request.getParameter("markdown") && Boolean.parseBoolean(request.getParameter("markdown"));
                String snString = request.getParameter("sn");
                if (null != snString) {
                    long sn = Long.parseLong(snString);
                    PsychologyReport report = Manager.getInstance().getPsychologyReport(token, sn, markdown);
                    if (null != report) {
                        this.respondOk(response, report.toJSON());
                    }
                    else {
                        this.respond(response, HttpStatus.NOT_FOUND_404);
                    }
                    this.complete();
                }
                else {
                    long cid = Long.parseLong(request.getParameter("cid"));
                    long start = Long.parseLong(request.getParameter("start"));
                    long end = Long.parseLong(request.getParameter("end"));
                    int page = Integer.parseInt(request.getParameter("page"));
                    JSONObject data = Manager.getInstance().getPsychologyReports(token, cid, start, end, page, markdown);
                    if (null != data) {
                        this.respondOk(response, data);
                    }
                    else {
                        this.respond(response, HttpStatus.NOT_FOUND_404);
                    }
                    this.complete();
                }
            } catch (Exception e) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
            }
        }
    }
}
