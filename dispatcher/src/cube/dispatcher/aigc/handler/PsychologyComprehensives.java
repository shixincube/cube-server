/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cube.aigc.psychology.ComprehensiveReport;
import cube.common.action.AIGCAction;
import cube.dispatcher.aigc.Manager;
import cube.util.FileLabels;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PsychologyComprehensives extends ContextHandler {

    public PsychologyComprehensives() {
        super("/aigc/psychology/comprehensive");
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
                JSONObject responseData = Manager.getInstance().syncRequest(token,
                        AIGCAction.GeneratePsychologyComprehensive, data);
                if (null == responseData) {
                    this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                    this.complete();
                    return;
                }

                ComprehensiveReport result = new ComprehensiveReport(responseData);

                JSONObject resultJson = result.toJSON();
                JSONArray array = resultJson.getJSONArray("comprehensives");
                for (int i = 0; i < array.length(); ++i) {
                    JSONObject comprehensive = array.getJSONObject(i);
                    if (comprehensive.has("files")) {
                        JSONArray fileArray = comprehensive.getJSONArray("files");
                        for (int j = 0; j < fileArray.length(); ++j) {
                            JSONObject fileJson = fileArray.getJSONObject(j);
                            FileLabels.reviseFileLabel(fileJson, token,
                                    Manager.getInstance().getPerformer().getExternalHttpEndpoint(),
                                    Manager.getInstance().getPerformer().getExternalHttpsEndpoint());
                        }
                    }
                }

                this.respondOk(response, resultJson);
                this.complete();
            } catch (Exception e) {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
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
                String snString = request.getParameter("sn");
                long sn = Long.parseLong(snString);
                JSONObject data = new JSONObject();
                data.put("sn", sn);
                JSONObject responseData = Manager.getInstance().syncRequest(token,
                        AIGCAction.QueryPsychologyComprehensive, data);
                if (null == responseData) {
                    this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                    this.complete();
                    return;
                }

                ComprehensiveReport result = new ComprehensiveReport(responseData);

                JSONObject resultJson = result.toJSON();
                JSONArray array = resultJson.getJSONArray("comprehensives");
                for (int i = 0; i < array.length(); ++i) {
                    JSONObject comprehensive = array.getJSONObject(i);
                    if (comprehensive.has("files")) {
                        JSONArray fileArray = comprehensive.getJSONArray("files");
                        for (int j = 0; j < fileArray.length(); ++j) {
                            JSONObject fileJson = fileArray.getJSONObject(j);
                            FileLabels.reviseFileLabel(fileJson, token,
                                    Manager.getInstance().getPerformer().getExternalHttpEndpoint(),
                                    Manager.getInstance().getPerformer().getExternalHttpsEndpoint());
                        }
                    }
                }

                this.respondOk(response, resultJson);
                this.complete();
            } catch (Exception e) {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
            }
        }
    }
}
