/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.robot.handler;

import cell.core.talk.dialect.ActionDialect;
import cube.dispatcher.Performer;
import cube.dispatcher.robot.RobotCellet;
import cube.robot.RobotAction;
import cube.robot.RobotStateCode;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 批量获取账号数据。
 */
public class GetAccountList extends ContextHandler {

    public GetAccountList(Performer performer) {
        super("/robot/account/list/");
        setHandler(new Handler(performer));
    }

    private class Handler extends RobotHandler {

        public Handler(Performer performer) {
            super(performer);
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            String code = this.getRequestPath(request);
            if (null == code || code.length() < 8) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            String beginStr = request.getParameter("begin");
            String endStr = request.getParameter("end");
            if (null == beginStr || null == endStr) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            int begin = 0;
            int end = 0;
            try {
                begin = Integer.parseInt(beginStr);
                end = Integer.parseInt(endStr);
                if (end <= begin) {
                    this.respond(response, HttpStatus.FORBIDDEN_403);
                    this.complete();
                    return;
                }
            } catch (Exception e) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            ActionDialect actionDialect = new ActionDialect(RobotAction.GetAccountList.name);
            actionDialect.addParam("begin", begin);
            actionDialect.addParam("end", end);
            ActionDialect responseDialect = this.performer.syncTransmit(RobotCellet.NAME, actionDialect);
            if (null == responseDialect) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
                return;
            }

            int stateCode = responseDialect.getParamAsInt("code");
            if (stateCode != RobotStateCode.Ok.code) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
                return;
            }

            JSONObject data = responseDialect.getParamAsJson("data");
            this.respondOk(response, data);
            this.complete();
        }
    }
}
