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

package cube.dispatcher.aigc;

import cell.core.talk.Primitive;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.common.JSONable;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.dispatcher.Performer;
import cube.dispatcher.PerformerListener;
import cube.dispatcher.aigc.handler.Conversation;
import cube.dispatcher.aigc.handler.*;
import cube.dispatcher.util.Tickable;
import cube.util.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 接口管理器。
 */
public class Manager implements Tickable, PerformerListener {

    private final static Manager instance = new Manager();

    private Performer performer;

    private Map<String, AuthToken> validTokenMap;
    private long lastClearToken;

    /**
     * Key：文件码
     */
    private Map<String, ASRFuture> asrFutureMap;

    public static Manager getInstance() {
        return Manager.instance;
    }

    public void start(Performer performer) {
        this.performer = performer;
        this.validTokenMap = new ConcurrentHashMap<>();
        this.asrFutureMap = new ConcurrentHashMap<>();

        this.setupHandler();

        this.performer.addTickable(this);
        this.performer.setListener(AIGCCellet.NAME, this);

        this.lastClearToken = System.currentTimeMillis();
    }

    public void stop() {
        this.performer.removeTickable(this);
    }

    private void setupHandler() {
        HttpServer httpServer = this.performer.getHttpServer();

        httpServer.addContextHandler(new RequestChannel());
        httpServer.addContextHandler(new Chat());
        httpServer.addContextHandler(new ImprovementChat());
        httpServer.addContextHandler(new NLGeneralTask());
        httpServer.addContextHandler(new Sentiment());
        httpServer.addContextHandler(new AutomaticSpeechRecognition());
        httpServer.addContextHandler(new Conversation());
    }

    public boolean checkToken(String token) {
        if (null == token) {
            return false;
        }

        if (this.validTokenMap.containsKey(token)) {
            return true;
        }

        JSONObject data = new JSONObject();
        data.put("token", token);
        Packet packet = new Packet(AIGCAction.CheckToken.name, data);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, packet.toDialect());
        if (null == response) {
            Logger.w(Manager.class, "#checkToken - Response is null : " + token);
            return false;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#checkToken - Response state is NOT ok : " + Packet.extractCode(responsePacket));
            return false;
        }

        AuthToken authToken = new AuthToken(Packet.extractDataPayload(responsePacket));
        this.validTokenMap.put(token, authToken);

        return true;
    }

    public AIGCChannel requestChannel(String participant) {
        JSONObject data = new JSONObject();
        data.put("participant", participant);
        Packet packet = new Packet(AIGCAction.RequestChannel.name, data);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, packet.toDialect());
        if (null == response) {
            Logger.w(Manager.class, "#requestChannel - Response is null : " + participant);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(Manager.class, "#requestChannel - Response state code is NOT Ok : " + participant +
                    " - " + Packet.extractCode(responsePacket));
            return null;
        }

        AIGCChannel channel = new AIGCChannel(Packet.extractDataPayload(responsePacket));
        return channel;
    }

    public AIGCChatRecord chat(String channelCode, String content, JSONArray records) {
        return this.chat(channelCode, content, null, records);
    }

    public AIGCChatRecord chat(String channelCode, String content, String desc, JSONArray records) {
        JSONObject data = new JSONObject();
        data.put("code", channelCode);
        data.put("content", content);
        if (null != desc) {
            data.put("desc", desc);
        }
        if (null != records) {
            data.put("records", records);
        }

        Packet packet = new Packet(AIGCAction.Chat.name, data);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, packet.toDialect(), 90 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#chat - Response is null - " + channelCode);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(Manager.class, "#chat - Response state code is NOT Ok - " + channelCode +
                    " - " + Packet.extractCode(responsePacket));
            return null;
        }

        AIGCChatRecord record = new AIGCChatRecord(Packet.extractDataPayload(responsePacket));
        return record;
    }

    /**
     * 增强型对话。
     *
     * @param channelCode
     * @param content
     * @param records
     * @return
     */
    public AIGCConversationResponse conversation(String channelCode, String content, JSONArray records) {
        JSONObject data = new JSONObject();
        data.put("code", channelCode);
        data.put("content", content);
        if (null != records) {
            data.put("records", records);
        }

        Packet packet = new Packet(AIGCAction.Conversation.name, data);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, packet.toDialect(), 120 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#conversation - Response is null - " + channelCode);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(Manager.class, "#conversation - Response state code is NOT Ok - " + channelCode +
                    " - " + Packet.extractCode(responsePacket));
            return null;
        }

        return new AIGCConversationResponse(Packet.extractDataPayload(responsePacket));
    }

    public SentimentResult sentimentAnalysis(String text) {
        JSONObject data = new JSONObject();
        data.put("text", text);
        Packet packet = new Packet(AIGCAction.Sentiment.name, data);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, packet.toDialect(), 120 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#sentimentAnalysis - Response is null");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(Manager.class, "#sentimentAnalysis - Response state code : "
                    + Packet.extractCode(responsePacket));
            return null;
        }

        SentimentResult result = new SentimentResult(Packet.extractDataPayload(responsePacket));
        return result;
    }

    public NLTask performNaturalLanguageTask(NLTask task) {
        // 检查任务
        if (!task.check()) {
            // 任务参数不正确
            Logger.w(Manager.class, "#performNaturalLanguageTask - task data error: " + task.type);
            return null;
        }

        Packet packet = new Packet(AIGCAction.NaturalLanguageTask.name, task.toJSON());
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, packet.toDialect(), 120 * 100);
        if (null == response) {
            Logger.w(Manager.class, "#performNaturalLanguageTask - Response is null");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(Manager.class, "#performNaturalLanguageTask - Response state code : "
                    + Packet.extractCode(responsePacket));
            return null;
        }

        return new NLTask(Packet.extractDataPayload(responsePacket));
    }

    public ASRFuture automaticSpeechRecognition(String domain, String fileCode) {
        if (this.asrFutureMap.containsKey(fileCode)) {
            // 正在处理
            return this.asrFutureMap.get(fileCode);
        }
        
        JSONObject data = new JSONObject();
        data.put("domain", domain);
        data.put("fileCode", fileCode);
        Packet packet = new Packet(AIGCAction.AutomaticSpeechRecognition.name, data);

        ASRFuture future = new ASRFuture(domain, fileCode);
        this.asrFutureMap.put(fileCode, future);

        this.performer.transmit(AIGCCellet.NAME, packet.toDialect());

        return future;
    }

    public ASRFuture queryASRFuture(String fileCode) {
        return this.asrFutureMap.get(fileCode);
    }

    @Override
    public void onTick(long now) {
        if (now - this.lastClearToken > 60 * 60 * 1000) {
            this.validTokenMap.clear();
            this.lastClearToken = now;

            Iterator<Map.Entry<String, ASRFuture>> iter = this.asrFutureMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, ASRFuture> e = iter.next();
                ASRFuture future = e.getValue();
                if (now - future.timestamp > 30 * 60 * 1000) {
                    iter.remove();
                }
            }
        }
    }

    @Override
    public void onReceived(String cellet, Primitive primitive) {
        ActionDialect actionDialect = new ActionDialect(primitive);
        String action = actionDialect.getName();

        if (AIGCAction.AutomaticSpeechRecognition.name.equals(action)) {
            Packet responsePacket = new Packet(actionDialect);
            // 状态码
            int stateCode = Packet.extractCode(responsePacket);
            if (stateCode == AIGCStateCode.Ok.code) {
                // 获取结果数据
                JSONObject resultJson = Packet.extractDataPayload(responsePacket);
                ASRResult result = new ASRResult(resultJson);
                ASRFuture future = this.asrFutureMap.get(result.getFileCode());
                if (null != future) {
                    future.result = result;
                    future.stateCode = stateCode;
                }
                else {
                    Logger.w(this.getClass(), "#onReceived - ASR result error: " + result.getFileCode());
                }
            }
            else {
                JSONObject resultJson = Packet.extractDataPayload(responsePacket);
                String fileCode = resultJson.getString("fileCode");
                ASRFuture future = this.asrFutureMap.get(fileCode);
                if (null != future) {
                    future.stateCode = stateCode;
                }
            }
        }
    }

    public class ASRFuture implements JSONable {

        protected final long timestamp;

        protected String domain;

        protected String fileCode;

        protected ASRResult result;

        protected int stateCode = -1;

        protected ASRFuture(String domain, String fileCode) {
            this.timestamp = System.currentTimeMillis();
            this.domain = domain;
            this.fileCode = fileCode;
        }

        @Override
        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("domain", this.domain);
            json.put("fileCode", this.fileCode);
            json.put("timestamp", this.timestamp);
            json.put("stateCode", this.stateCode);
            if (null != this.result) {
                json.put("result", this.result.toJSON());
            }
            return json;
        }

        @Override
        public JSONObject toCompactJSON() {
            return this.toJSON();
        }
    }
}