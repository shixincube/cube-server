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

import cube.aigc.psychology.composition.AnswerSheet;
import cube.aigc.psychology.composition.Scale;
import cube.aigc.psychology.composition.ScaleResult;
import cube.dispatcher.aigc.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 心理学量表数据。
 */
public class PsychologyScaleOperation extends ContextHandler {

    public PsychologyScaleOperation() {
        super("/aigc/psychology/scale");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        public Handler() {
            super();
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            // 获取指定 SN 量表
            String token = this.getLastRequestPath(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            try {
                long sn = Long.parseLong(request.getParameter("sn"));
                Scale scale = Manager.getInstance().getPsychologyScale(token, sn);
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

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            // 提交答题卡
            String token = this.getLastRequestPath(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            try {
                JSONObject data = this.readBodyAsJSONObject(request);
                AnswerSheet sheet = new AnswerSheet(data);
                ScaleResult scaleResult = Manager.getInstance().submitPsychologyAnswerSheet(token, sheet);
                if (null != scaleResult) {
                    this.respondOk(response, scaleResult.toJSON());
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
