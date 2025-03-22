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
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.AIGCChatHistory;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 查询 Chat 历史数据任务。
 */
public class QueryChatHistoryTask extends ServiceTask {

    public QueryChatHistoryTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        String tokenCode = this.getTokenCode(dialect);
        if (null == tokenCode) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.NoToken.code, new JSONObject()));
            markResponseTime();
            return;
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();
        AuthToken token = service.getToken(tokenCode);
        if (null == token) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InconsistentToken.code, new JSONObject()));
            markResponseTime();
            return;
        }

        String channel = null;
        long contactId = 0;
        int feedback = -1;
        long start = 0;
        long end = 0;

        try {
            if (packet.data.has("channel")) {
                channel = packet.data.getString("channel");
            }
            if (packet.data.has("contactId")) {
                contactId = packet.data.getLong("contactId");
            }
            if (packet.data.has("feedback")) {
                feedback = packet.data.getInt("feedback");
            }

            start = packet.data.getLong("start");

            if (packet.data.has("end")) {
                end = packet.data.getLong("end");
            }
            else {
                end = System.currentTimeMillis();
            }
        } catch (Exception e) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        List<AIGCChatHistory> historyList = null;

        if (null != channel) {
            historyList = service.getStorage().readHistoriesByChannel(channel, start, end);
        }
        else if (contactId != 0) {
            historyList = service.getStorage().readHistoriesByContactId(contactId, token.getDomain(), start, end);
        }
        else if (feedback != -1) {
            historyList = service.getStorage().readHistoriesByFeedback(feedback, token.getDomain(), start, end);
        }

        if (null == historyList) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
            markResponseTime();
            return;
        }

        Collections.sort(historyList, new Comparator<AIGCChatHistory>() {
            @Override
            public int compare(AIGCChatHistory h1, AIGCChatHistory h2) {
                return (int)(h1.queryTime - h2.queryTime);
            }
        });

        JSONArray array = new JSONArray();
        for (AIGCChatHistory history : historyList) {
            array.put(history.toJSON());
        }

        JSONObject data = new JSONObject();
        data.put("start", start);
        data.put("end", end);
        data.put("total", historyList.size());
        data.put("histories", array);

        this.cellet.speak(this.talkContext,
                this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, data));
        markResponseTime();
    }
}
