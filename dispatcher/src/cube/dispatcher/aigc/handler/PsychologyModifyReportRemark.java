/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cube.dispatcher.aigc.Manager;
import cube.util.FileLabels;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 心理学基线数据。
 */
public class PsychologyModifyReportRemark extends ContextHandler {

    public PsychologyModifyReportRemark() {
        super("/aigc/psychology/report/remark");
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
                JSONObject result = Manager.getInstance().modifyReportRemark(token, data);
                if (null != result) {
                    if (result.has("fileLabel")) {
                        FileLabels.reviseFileLabel(result.getJSONObject("fileLabel"), token,
                                Manager.getInstance().getPerformer().getExternalHttpEndpoint(),
                                Manager.getInstance().getPerformer().getExternalHttpsEndpoint());
                    }
                    result.remove("evaluation");
                    result.remove("personality");
                    this.respondOk(response, result);
                }
                else {
                    this.respond(response, HttpStatus.NOT_FOUND_404, this.makeError(HttpStatus.NOT_FOUND_404));
                }
                this.complete();
            } catch (Exception e) {
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
            }
        }
    }
}
