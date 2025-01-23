/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2020-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cube.aigc.psychology.algorithm.Attention;
import cube.dispatcher.aigc.Manager;
import cube.dispatcher.util.FileLabels;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 重置报告关注等级。
 */
public class ResetReportAttention extends ContextHandler {

    public ResetReportAttention() {
        super("/aigc/psychology/report/reset");
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
                long sn = data.getLong("sn");
                Attention attention = data.has("attention") ?
                        Attention.parse(data.getInt("attention")) : null;
                JSONObject responseData = Manager.getInstance().resetReportAttention(token, sn, attention);
                if (null != responseData) {
                    if (responseData.has("fileLabel")) {
                        FileLabels.reviseFileLabel(responseData.getJSONObject("fileLabel"), token,
                                Manager.getInstance().getPerformer().getExternalHttpEndpoint(),
                                Manager.getInstance().getPerformer().getExternalHttpsEndpoint());
                    }

                    this.respondOk(response, responseData);
                    this.complete();
                }
                else {
                    this.respond(response, HttpStatus.NOT_FOUND_404);
                    this.complete();
                }
            } catch (Exception e) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
            }
        }
    }
}
