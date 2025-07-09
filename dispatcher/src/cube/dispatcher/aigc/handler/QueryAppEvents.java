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
            String token = this.getApiToken(request);
            if (!Manager.getInstance().checkToken(token, this.getDevice(request))) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
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
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
                return;
            }

            JSONObject responseData = Manager.getInstance().queryAppEvents(token, contactId, event,
                    start, end, pageIndex, pageSize);
            if (null == responseData) {
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
                return;
            }

            this.respondOk(response, responseData);
            this.complete();
        }
    }
}
