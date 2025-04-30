/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cube.aigc.PromptRecord;
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
            String token = this.getApiToken(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                this.complete();
                return;
            }

            JSONObject result = Manager.getInstance().getPrompts(token);
            if (null == result) {
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
                return;
            }

            this.respondOk(response, result);
            this.complete();
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getApiToken(request);
            if (!Manager.getInstance().checkToken(token)) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
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

                    List<PromptRecord> promptRecordList = new ArrayList<>();
                    for (int i = 0; i < promptArray.length(); ++i) {
                        PromptRecord promptRecord = new PromptRecord(promptArray.getJSONObject(i));
                        promptRecordList.add(promptRecord);
                    }

                    List<Long> contactIdList = null;
                    if (null != contactIdArray) {
                        contactIdList = new ArrayList<>();
                        for (int i = 0; i < contactIdArray.length(); ++i) {
                            contactIdList.add(contactIdArray.getLong(i));
                        }
                    }

                    if (Manager.getInstance().addPrompts(token, promptRecordList, contactIdList)) {
                        JSONObject responseData = new JSONObject();
                        responseData.put("total", promptRecordList.size() * (null != contactIdList ? contactIdList.size() : 1));
                        this.respondOk(response, responseData);
                        this.complete();
                    }
                    else {
                        this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                        this.complete();
                    }
                } catch (Exception e) {
                    this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
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
                        this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                        this.complete();
                    }
                } catch (Exception e) {
                    this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                    this.complete();
                }
            }
            else if (action.equalsIgnoreCase("update")) {
                try {
                    JSONObject data = readBodyAsJSONObject(request);
                    PromptRecord promptRecord = new PromptRecord(data);
                    if (Manager.getInstance().updatePrompt(token, promptRecord)) {
                        JSONObject responseData = new JSONObject();
                        responseData.put("total", 1);
                        this.respondOk(response, responseData);
                        this.complete();
                    }
                    else {
                        this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                        this.complete();
                    }
                } catch (Exception e) {
                    this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                    this.complete();
                }
            }
            else {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
            }
        }
    }
}
