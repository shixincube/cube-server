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
 * 获取账号数据。
 */
public class GetAccount extends ContextHandler {

    public GetAccount(Performer performer) {
        super("/robot/account/");
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

            String idString = request.getParameter("id");
            if (null == idString) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            long accountId = 0;
            try {
                accountId = Long.parseLong(idString);
            } catch (Exception e) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            ActionDialect actionDialect = new ActionDialect(RobotAction.GetAccount.name);
            actionDialect.addParam("accountId", accountId);
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
