/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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

            this.performer.execute(new Runnable() {
                @Override
                public void run() {
                    Manager.getInstance().callback(eventName, eventData);
                }
            });

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
