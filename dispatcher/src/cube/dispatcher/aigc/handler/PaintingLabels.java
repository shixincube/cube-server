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
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 绘画标签操作。
 */
public class PaintingLabels extends ContextHandler {

    public PaintingLabels() {
        super("/aigc/painting/label");
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

                JSONObject data = Manager.getInstance().getPaintingLabels(token, sn);
                if (null == data) {
                    this.respond(response, HttpStatus.NOT_FOUND_404);
                    this.complete();
                    return;
                }

                this.respondOk(response, data);
                this.complete();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#doGet", e);
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
            }
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
                JSONArray labels = data.getJSONArray("labels");

                if (Manager.getInstance().submitPaintingLabels(token, sn, labels)) {
                    JSONObject responseData = new JSONObject();
                    responseData.put("sn", sn);
                    this.respondOk(response, responseData);
                }
                else {
                    this.respond(response, HttpStatus.BAD_REQUEST_400);
                }

                this.complete();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#doPost", e);
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
            }
        }
    }
}
