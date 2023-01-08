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

package cube.dispatcher.robot;

import cell.core.talk.dialect.ActionDialect;
import cube.dispatcher.Performer;
import cube.robot.RobotAction;
import cube.util.CrossDomainHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Roboengine 事件监听处理器。
 */
public class RoboengineEventHandler extends CrossDomainHandler {

    public final static String CONTEXT_PATH = "/robot/event/";

    private String token = "kLBNGSmrTbmNlBfTYcIaKqYQiDPKoTkE";

    private Performer performer;

    public RoboengineEventHandler(Performer performer) {
        this.performer = performer;
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        String token = extractToken(request);
        if (null == token) {
            this.respond(response, HttpStatus.FORBIDDEN_403);
            this.complete();
            return;
        }

        if (!this.token.equals(token)) {
            this.respond(response, HttpStatus.FORBIDDEN_403);
            this.complete();
            return;
        }

        try {
            JSONObject data = this.readBodyAsJSONObject(request);
            String eventName = data.getString("name");
            JSONObject eventData = data.getJSONObject("data");

            ActionDialect dialect = new ActionDialect(RobotAction.Event.name);
            dialect.addParam("name", eventName);
            dialect.addParam("data", eventData);
            this.performer.transmit(RobotCellet.NAME, dialect);
        } catch (IOException e) {
            this.respond(response, HttpStatus.BAD_REQUEST_400);
        }

        this.complete();
    }

    private String extractToken(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path.length() < 32) {
            return null;
        }
        return path.substring(1, 33);
    }
}
