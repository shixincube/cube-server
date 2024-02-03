/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

import cube.dispatcher.aigc.AccessController;
import cube.dispatcher.aigc.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 图像物体识别。
 */
public class ObjectDetection extends ContextHandler {

    public ObjectDetection() {
        super("/aigc/cv/detection");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        private AccessController controller;

        public Handler() {
            super();
            this.controller = new AccessController();
            this.controller.setEachIPInterval(200);
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            if (!this.controller.filter(request)) {
                this.respond(response, HttpStatus.NOT_ACCEPTABLE_406);
                this.complete();
                return;
            }

            String token = this.getRequestPath(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            String channelCode = request.getParameter("cc");
            if (null == channelCode) {
                channelCode = request.getParameter("code");
            }

            if (null == channelCode) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            Manager.ObjectDetectionFuture future = Manager.getInstance().getObjectDetectionFuture(channelCode);
            if (null == future) {
                this.respond(response, HttpStatus.NOT_FOUND_404);
                this.complete();
                return;
            }

            this.respondOk(response, future.toJSON());
            this.complete();
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            if (!this.controller.filter(request)) {
                this.respond(response, HttpStatus.NOT_ACCEPTABLE_406);
                this.complete();
                return;
            }

            String token = this.getLastRequestPath(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            try {
                JSONObject json = this.readBodyAsJSONObject(request);
                String code = json.getString("code");
                JSONArray fileCodeList = json.getJSONArray("fileCodeList");
                Manager.ObjectDetectionFuture future = Manager.getInstance().objectDetection(token, code, fileCodeList);
                if (null == future) {
                    // 故障
                    this.respond(response, HttpStatus.BAD_REQUEST_400);
                    this.complete();
                    return;
                }

                this.respondOk(response, future.toJSON());
                this.complete();
            } catch (Exception e) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
            }
        }
    }
}
