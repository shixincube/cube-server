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

        public void doPost(HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            String code = this.getRequestPath(request);
            if (null == code || code.length() < 8) {
                this.respond(response, HttpStatus.NOT_ACCEPTABLE_406);
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