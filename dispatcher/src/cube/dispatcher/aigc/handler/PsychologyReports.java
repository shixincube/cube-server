/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cell.util.log.Logger;
import cube.aigc.psychology.*;
import cube.dispatcher.aigc.Manager;
import cube.util.FileLabels;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 心理学绘画报告。
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
            String token = this.getApiToken(request);
            if (!Manager.getInstance().checkToken(token, this.getDevice(request))) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                this.complete();
                return;
            }

            try {
                JSONObject data = this.readBodyAsJSONObject(request);

                if (data.has("fileCode")) {
                    Attribute attribute = new Attribute(data.getJSONObject("attribute"));
                    String fileCode = data.getString("fileCode");
                    String theme = data.has("theme") ? data.getString("theme") : Theme.Generic.code;
                    int indicatorTexts = data.has("indicatorTexts") ? data.getInt("indicatorTexts") : 10;

                    PaintingReport report =
                            Manager.getInstance().generatePsychologyReport(request.getRemoteHost(), token, attribute,
                                    fileCode, theme, indicatorTexts);
                    if (null != report) {
                        JSONObject responseData = report.toJSON();
                        if (responseData.has("fileLabel")) {
                            FileLabels.reviseFileLabel(responseData.getJSONObject("fileLabel"), token,
                                    Manager.getInstance().getPerformer().getExternalHttpEndpoint(),
                                    Manager.getInstance().getPerformer().getExternalHttpsEndpoint());
                        }
                        this.respondOk(response, responseData);
                    }
                    else {
                        this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                    }
                }
                else if (data.has("scaleSn")) {
                    ScaleReport report = Manager.getInstance().generatePsychologyReport(request.getRemoteHost(), token,
                            data.getLong("scaleSn"));
                    if (null != report) {
                        this.respondOk(response, report.toJSON());
                    }
                    else {
                        this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                    }
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
            if (!Manager.getInstance().checkToken(token, this.getDevice(request))) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                this.complete();
                return;
            }

            try {
                boolean markdown = (null != request.getParameter("markdown"))
                        && Boolean.parseBoolean(request.getParameter("markdown"));
                String snString = request.getParameter("sn");
                if (null != snString) {
                    long sn = Long.parseLong(snString);
                    Report report = Manager.getInstance().getPsychologyReport(token, sn, markdown);
                    if (null != report) {
                        JSONObject responseData = report.toJSON();
                        if (responseData.has("fileLabel")) {
                            FileLabels.reviseFileLabel(responseData.getJSONObject("fileLabel"), token,
                                    Manager.getInstance().getPerformer().getExternalHttpEndpoint(),
                                    Manager.getInstance().getPerformer().getExternalHttpsEndpoint());
                        }
                        this.respondOk(response, responseData);
                    }
                    else {
                        this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                    }
                    this.complete();
                }
                else {
                    String type = (null != request.getParameter("type")) ?
                            request.getParameter("type") : "painting";
                    int page = Integer.parseInt(request.getParameter("page"));
                    int size = Integer.parseInt(request.getParameter("size"));
                    boolean descending = null == request.getParameter("desc")
                            || Boolean.parseBoolean(request.getParameter("desc"));
                    JSONObject data = Manager.getInstance().getPsychologyReports(token, type, page, size, descending);
                    if (null != data) {
                        if (data.has("list")) {
                            JSONArray array = data.getJSONArray("list");
                            for (int i = 0; i < array.length(); ++i) {
                                JSONObject item = array.getJSONObject(i);
                                if (item.has("fileLabel")) {
                                    FileLabels.reviseFileLabel(item.getJSONObject("fileLabel"), token,
                                            Manager.getInstance().getPerformer().getExternalHttpEndpoint(),
                                            Manager.getInstance().getPerformer().getExternalHttpsEndpoint());
                                }
                            }
                        }
                        this.respondOk(response, data);
                    }
                    else {
                        this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                    }
                    this.complete();
                }
            } catch (Exception e) {
                Logger.e(this.getClass(), "#doGet", e);
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
            }
        }
    }
}
