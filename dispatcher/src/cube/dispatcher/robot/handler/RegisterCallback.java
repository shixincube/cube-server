/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.robot.handler;

import cube.dispatcher.Performer;
import cube.dispatcher.robot.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

/**
 * 注册回调 URL 链接。
 */
public class RegisterCallback extends ContextHandler {

    public RegisterCallback(Performer performer) {
        super("/robot/register/");
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

            ArrayList<String> list = new ArrayList();
            if (data.has("url")) {
                list.add(data.getString("url"));
            }
            else if (data.has("urls")) {
                JSONArray array = data.getJSONArray("urls");
                for (int i = 0; i < array.length(); ++i) {
                    list.add(array.getString(i));
                }
            }

            for (String url : list) {
                Manager.getInstance().registerCallback(url);
            }

            this.respond(response, HttpStatus.OK_200);
            this.complete();
        }
    }
}
