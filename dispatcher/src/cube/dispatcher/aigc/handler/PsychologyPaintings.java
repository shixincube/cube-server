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

public class PsychologyPaintings extends ContextHandler {

    public PsychologyPaintings() {
        super("/aigc/psychology/painting");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        public Handler() {
            super();
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            // 量表列表
            String token = this.getLastRequestPath(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            try {
                String fileCode = request.getParameter("fc");
                if (null != fileCode) {
                    JSONObject result = Manager.getInstance().getPsychologyPainting(token, fileCode);
                    if (null != result) {
                        this.respondOk(response, result);
                    }
                    else {
                        this.respond(response, HttpStatus.NOT_FOUND_404);
                    }
                }
                else {
                    long sn = Long.parseLong(request.getParameter("sn"));
                    boolean bbox = null == request.getParameter("bbox") || Boolean.parseBoolean(request.getParameter("bbox"));
                    boolean vparam = null != request.getParameter("vparam") && Boolean.parseBoolean(request.getParameter("vparam"));
                    double prob = (null != request.getParameter("prob")) ?
                            Double.parseDouble(request.getParameter("prob")) : 0.5d;
                    JSONObject result = Manager.getInstance().getPsychologyPainting(token, sn, bbox, vparam, prob);
                    if (null != result) {
                        if (result.has("fileCode") && result.has("fileURL")) {
                            FileLabels.reviseFileLabel(result, token,
                                    Manager.getInstance().getPerformer().getExternalHttpEndpoint(),
                                    Manager.getInstance().getPerformer().getExternalHttpsEndpoint());
                        }

                        this.respondOk(response, result);
                    }
                    else {
                        this.respond(response, HttpStatus.NOT_FOUND_404);
                    }
                }
                this.complete();
            } catch (Exception e) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
            }
        }
    }
}
