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
 * 通过指定模块进行任务推算。
 * @deprecated
 */
public class InferByModule extends ContextHandler {

    public InferByModule() {
        super("/aigc/module/infer/");
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
                String module = data.getString("module");
                JSONObject param = data.getJSONObject("param");
                JSONObject responseData = null;//Manager.getInstance().inferByModule(token, module, param);
                if (null != responseData) {
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
