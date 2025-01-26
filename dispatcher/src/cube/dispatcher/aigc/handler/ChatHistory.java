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
 * 操作 Chat 历史数据。
 */
public class ChatHistory extends ContextHandler {

    public ChatHistory() {
        super("/aigc/history/");
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

            String channel = null;
            long contactId = 0;
            int feedback = -1;
            long start = 0;
            long end = 0;
            try {
                if (null != request.getParameter("channel")) {
                    channel = request.getParameter("channel");
                }

                if (null != request.getParameter("cid")) {
                    contactId = Long.parseLong(request.getParameter("cid"));
                }
                if (null != request.getParameter("feedback")) {
                    feedback = Integer.parseInt(request.getParameter("feedback"));
                }

                start = Long.parseLong(request.getParameter("start"));

                if (null != request.getParameter("end")) {
                    end = Long.parseLong(request.getParameter("end"));
                }
                else {
                    end = System.currentTimeMillis();
                }
            } catch (Exception e) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            JSONObject responseData = Manager.getInstance().queryChatHistory(token, channel,
                    contactId, feedback, start, end);
            if (null == responseData) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
                return;
            }

            this.respondOk(response, responseData);
            this.complete();
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getLastRequestPath(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            long sn = 0;
            int feedback = -1;

            try {
                JSONObject data = this.readBodyAsJSONObject(request);
                sn = data.getLong("sn");
                feedback = data.getInt("feedback");
            } catch (Exception e) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            if (Manager.getInstance().evaluate(token, sn, feedback)) {
                JSONObject data = new JSONObject();
                data.put("sn", sn);
                data.put("feedback", feedback);
                this.respondOk(response, data);
                this.complete();
            }
            else {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
            }
        }
    }
}
