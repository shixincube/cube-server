/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cube.aigc.psychology.composition.Scale;
import cube.dispatcher.aigc.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 心理学量表数据。
 */
public class PsychologyScales extends ContextHandler {

    public PsychologyScales() {
        super("/aigc/psychology/scales");
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
                JSONObject result = Manager.getInstance().listPsychologyScales(token);
                if (null != result) {
                    this.respondOk(response, result);
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
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getLastRequestPath(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            try {
                JSONObject data = this.readBodyAsJSONObject(request);
                String scaleName = data.getString("name");
                String gender = data.getString("gender");
                int age = data.getInt("age");
                Scale scale = Manager.getInstance().generatePsychologyScale(token, scaleName, gender, age);
                if (null != scale) {
                    this.respondOk(response, scale.toJSON());
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
