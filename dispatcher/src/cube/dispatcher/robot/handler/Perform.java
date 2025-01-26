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
 * 执行指定任务。
 */
public class Perform extends ContextHandler {

    public Perform(Performer performer) {
        super("/robot/perform/");
        setHandler(new Handler(performer));
    }

    private class Handler extends RobotHandler {

        public Handler(Performer performer) {
            super(performer);
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            String code = this.getRequestPath(request);
            if (null == code || code.length() < 8) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            JSONObject data = this.readBodyAsJSONObject(request);
            if (null == data) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            if (!data.has("name")) {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
                return;
            }

            String name = data.getString("name");
            JSONObject parameter = data.has("parameter") ? data.getJSONObject("parameter") : new JSONObject();
            long accountId = data.has("accountId") ? data.getLong("accountId") : 0;

            ActionDialect dialect = new ActionDialect(RobotAction.Perform.name);
            dialect.addParam("name", name);
            dialect.addParam("parameter", parameter);
            if (accountId > 0) {
                dialect.addParam("accountId", accountId);
            }

            ActionDialect responseDialect = this.performer.syncTransmit(RobotCellet.NAME, dialect);
            if (null == responseDialect) {
                this.respond(response, HttpStatus.NOT_FOUND_404);
                this.complete();
                return;
            }

            int stateCode = responseDialect.getParamAsInt("code");
            if (stateCode != RobotStateCode.Ok.code) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
                return;
            }

            this.respondOk(response, data);
            this.complete();
        }
    }
}
