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

import cube.dispatcher.aigc.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 查询应用事件。
 */
public class QueryAppEvents extends ContextHandler {

    public QueryAppEvents() {
        super("/aigc/event/");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {
        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getLastRequestPath(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            long contactId;
            String event;
            long start;
            long end = System.currentTimeMillis();
            int pageIndex = 0;
            int pageSize = 10;
            try {
                contactId = Long.parseLong(request.getParameter("cid"));
                event = request.getParameter("event");
                start = Long.parseLong(request.getParameter("start"));
                // 以下是可选参数
                String param = request.getParameter("end");
                if (null != param) {
                    end = Long.parseLong(param);
                }
                param = request.getParameter("pi");
                if (null != param) {
                    pageIndex = Integer.parseInt(param);
                }
                param = request.getParameter("ps");
                if (null != param) {
                    pageSize = Integer.parseInt(param);
                }
            } catch (Exception e) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            JSONObject responseData = Manager.getInstance().queryAppEvents(token, contactId, event,
                    start, end, pageIndex, pageSize);
            if (null == responseData) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
                return;
            }

            this.respondOk(response, responseData);
            this.complete();
        }
    }
}
