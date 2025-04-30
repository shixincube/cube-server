/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.aigc.PromptRecord;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 设置提示词。
 */
public class SetPromptsTask extends ServiceTask {

    public SetPromptsTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        AuthToken authToken = extractAuthToken(dialect);
        if (null == authToken) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        String action = packet.data.getString("action");
        JSONObject responsePayload = new JSONObject();
        responsePayload.put("action", action);

        AIGCService service = ((AIGCCellet) this.cellet).getService();

        if (action.equalsIgnoreCase("add")) {
            JSONArray promptArray = null;
            if (packet.data.has("prompts")) {
                promptArray = packet.data.getJSONArray("prompts");
            }
            else {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
                markResponseTime();
                return;
            }

            JSONArray contactIdArray = packet.data.has("contactIds") ?
                    packet.data.getJSONArray("contactIds") : null;
            if (null == contactIdArray) {
                contactIdArray = new JSONArray();
                contactIdArray.put(0);
            }

            List<PromptRecord> promptRecordList = new ArrayList<>();
            for (int i = 0; i < promptArray.length(); ++i) {
                PromptRecord promptRecord = new PromptRecord(promptArray.getJSONObject(i));
                promptRecordList.add(promptRecord);
            }

            List<Long> contactIdList = new ArrayList<>();
            for (int i = 0; i < contactIdArray.length(); ++i) {
                contactIdList.add(contactIdArray.getLong(i));
            }

            service.getStorage().writePrompts(promptRecordList, contactIdList);
            responsePayload.put("total", promptRecordList.size() * contactIdList.size());
        }
        else if (action.equalsIgnoreCase("remove")) {
            if (packet.data.has("contactId") && packet.data.has("title")) {
                long contactId = packet.data.getLong("contactId");
                String title = packet.data.getString("title");

                int total = service.getStorage().deletePrompt(contactId, title);
                responsePayload.put("total", total);
            }
            else if (packet.data.has("idList")) {
                JSONArray idArray = packet.data.getJSONArray("idList");
                List<Long> idList = new ArrayList<>();
                for (int i = 0; i < idArray.length(); ++i) {
                    idList.add(idArray.getLong(i));
                }

                service.getStorage().deletePrompts(idList);
                responsePayload.put("total", idList.size());
            }
            else {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
                markResponseTime();
                return;
            }
        }
        else if (action.equalsIgnoreCase("update")) {
            long contactId = packet.data.has("contactId") ?
                    packet.data.getLong("contactId") : authToken.getContactId();

            if (packet.data.has("id") &&
                    packet.data.has("title") &&
                    packet.data.has("content") &&
                    packet.data.has("act")) {
                PromptRecord promptRecord = new PromptRecord(packet.data.getLong("id"),
                        packet.data.getString("title"),
                        packet.data.getString("content"),
                        packet.data.getString("act"), false);

                service.getStorage().writePrompt(promptRecord, contactId);
                responsePayload.put("total", 1);
            }
            else {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
                markResponseTime();
                return;
            }
        }

        this.cellet.speak(this.talkContext,
                this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, responsePayload));
        markResponseTime();
    }
}
