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

package cube.dispatcher.aigc.handler;

import cube.aigc.Prompt;
import cube.dispatcher.aigc.Manager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

public class Prompts extends ContextHandler {

    public Prompts() {
        super("/aigc/prompt/");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        public Handler() {
            super();
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getRequestPath(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401);
                this.complete();
                return;
            }

            JSONObject result = Manager.getInstance().getPrompts(token);
            if (null == result) {
                this.respond(response, HttpStatus.BAD_REQUEST_400);
                this.complete();
                return;
            }

            this.respondOk(response, result);
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

            String action = this.getRequestPath(request);
            if (action.equalsIgnoreCase("add")) {
                try {
                    JSONObject data = readBodyAsJSONObject(request);

                    JSONArray promptArray = null;
                    if (data.has("prompts")) {
                        promptArray = data.getJSONArray("prompts");
                    }
                    else {
                        promptArray = new JSONArray();
                        promptArray.put(data.getJSONObject("prompt"));
                    }

                    JSONArray contactIdArray = null;
                    if (data.has("contactIds")) {
                        contactIdArray = data.getJSONArray("contactIds");
                    }
                    else if (data.has("contactId")) {
                        contactIdArray = new JSONArray();
                        contactIdArray.put(data.getLong("contactId"));
                    }

                    List<Prompt> promptList = new ArrayList<>();
                    for (int i = 0; i < promptArray.length(); ++i) {
                        Prompt prompt = new Prompt(promptArray.getJSONObject(i));
                        promptList.add(prompt);
                    }

                    List<Long> contactIdList = null;
                    if (null != contactIdArray) {
                        contactIdList = new ArrayList<>();
                        for (int i = 0; i < contactIdArray.length(); ++i) {
                            contactIdList.add(contactIdArray.getLong(i));
                        }
                    }

                    if (Manager.getInstance().addPrompts(token, promptList, contactIdList)) {
                        JSONObject responseData = new JSONObject();
                        responseData.put("total", promptList.size() * (null != contactIdList ? contactIdList.size() : 1));
                        this.respondOk(response, responseData);
                        this.complete();
                    }
                    else {
                        this.respond(response, HttpStatus.BAD_REQUEST_400);
                        this.complete();
                    }
                } catch (Exception e) {
                    this.respond(response, HttpStatus.FORBIDDEN_403);
                    this.complete();
                }
            }
            else if (action.equalsIgnoreCase("remove")) {
                try {
                    JSONObject data = readBodyAsJSONObject(request);

                    JSONArray idArray = data.getJSONArray("idList");
                    List<Long> idList = new ArrayList<>();
                    for (int i = 0; i < idArray.length(); ++i) {
                        idList.add(idArray.getLong(i));
                    }

                    if (Manager.getInstance().removePrompts(token, idList)) {
                        JSONObject responseData = new JSONObject();
                        responseData.put("total", idList.size());
                        this.respondOk(response, responseData);
                        this.complete();
                    }
                    else {
                        this.respond(response, HttpStatus.BAD_REQUEST_400);
                        this.complete();
                    }
                } catch (Exception e) {
                    this.respond(response, HttpStatus.FORBIDDEN_403);
                    this.complete();
                }
            }
            else {
                this.respond(response, HttpStatus.FORBIDDEN_403);
                this.complete();
            }
        }
    }
}
