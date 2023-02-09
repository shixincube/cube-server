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
