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
import cube.aigc.ConfigInfo;
import cube.aigc.ModelConfig;
import cube.aigc.Prompt;
import cube.aigc.attachment.ui.Event;
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
import cube.dispatcher.aigc.handler.app.App;
import cube.dispatcher.util.Tickable;
import cube.util.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 接口管理器。
 */
public class Manager implements Tickable, PerformerListener {

    private final static Manager instance = new Manager();

    private Performer performer;

    private Map<String, ContactToken> validTokenMap;
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

        (new Thread() {
            @Override
            public void run() {
                App.getInstance().start();
            }
        }).start();
    }

    public void stop() {
        this.performer.removeTickable(this);

        App.getInstance().stop();
    }

    private void setupHandler() {
        HttpServer httpServer = this.performer.getHttpServer();

        httpServer.addContextHandler(new RequestChannel());
        httpServer.addContextHandler(new Chat());
        httpServer.addContextHandler(new NLGeneralTask());
        httpServer.addContextHandler(new Sentiment());
        httpServer.addContextHandler(new Summarization());
        httpServer.addContextHandler(new AutomaticSpeechRecognition());
        httpServer.addContextHandler(new Conversation());
        httpServer.addContextHandler(new KnowledgeDocs());
        httpServer.addContextHandler(new ImportKnowledgeDoc());
        httpServer.addContextHandler(new RemoveKnowledgeDoc());
        httpServer.addContextHandler(new SearchResults());
        httpServer.addContextHandler(new ChartData());
        httpServer.addContextHandler(new Prompts());
        httpServer.addContextHandler(new SubmitEvent());
        httpServer.addContextHandler(new PreInfer());

        httpServer.addContextHandler(new cube.dispatcher.aigc.handler.app.Session());
        httpServer.addContextHandler(new cube.dispatcher.aigc.handler.app.Verify());
        httpServer.addContextHandler(new cube.dispatcher.aigc.handler.app.Config());
        httpServer.addContextHandler(new cube.dispatcher.aigc.handler.app.Change());
        httpServer.addContextHandler(new cube.dispatcher.aigc.handler.app.Chat());
        httpServer.addContextHandler(new cube.dispatcher.aigc.handler.app.Evaluate());
        httpServer.addContextHandler(new cube.dispatcher.aigc.handler.app.KeepAlive());
    }

    public boolean checkToken(String token) {
        return (null != this.checkAndGetToken(token));
    }

    public String checkAndGetToken(String token) {
        if (null == token) {
            return null;
        }

        if (this.validTokenMap.containsKey(token)) {
            ContactToken contactToken = this.validTokenMap.get(token);
            return contactToken.authToken.getCode();
        }

        JSONObject data = new JSONObject();

        // 如果是6位，则视为邀请码
        if (token.length() == 6) {
            data.put("invitation", token);
        }
        else {
            data.put("token", token);
        }
        Packet packet = new Packet(AIGCAction.CheckToken.name, data);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, packet.toDialect());
        if (null == response) {
            Logger.w(Manager.class, "#checkToken - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#checkToken - Response state is NOT ok : " + Packet.extractCode(responsePacket));
            return null;
        }

        JSONObject payload = Packet.extractDataPayload(responsePacket);
        AuthToken authToken = new AuthToken(payload.getJSONObject("token"));
        token = authToken.getCode();
        Contact contact = new Contact(payload.getJSONObject("contact"));
        this.validTokenMap.put(token, new ContactToken(authToken, contact));

        return token;
    }

    public ContactToken getContactToken(String token) {
        return this.validTokenMap.get(token);
    }

    public ConfigInfo getConfigInfo(String token) {
        JSONObject data = new JSONObject();
        data.put("token", token);
        Packet packet = new Packet(AIGCAction.GetConfig.name, data);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, packet.toDialect());
        if (null == response) {
            Logger.w(Manager.class, "#getConfigData - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#getConfigData - Response state is NOT ok : " + Packet.extractCode(responsePacket));
            return null;
        }

        return new ConfigInfo(Packet.extractDataPayload(responsePacket));
    }

    public ModelConfig getModelConfig(ConfigInfo configInfo, String modelName) {
        for (ModelConfig config : configInfo.models) {
            if (config.getName().equals(modelName)) {
                return config;
            }
        }

        return null;
    }

    public KnowledgeProfile getKnowledgeProfile(String token) {
        Packet packet = new Packet(AIGCAction.GetKnowledgeProfile.name, new JSONObject());
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(Manager.class, "#getKnowledgeProfile - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#getKnowledgeProfile - Response state is NOT ok : " + Packet.extractCode(responsePacket));
            return null;
        }

        return new KnowledgeProfile(Packet.extractDataPayload(responsePacket));
    }

    public JSONObject getKnowledgeDocs(String token) {
        Packet packet = new Packet(AIGCAction.ListKnowledgeDocs.name, new JSONObject());
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(Manager.class, "#getKnowledgeDocs - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#getKnowledgeDocs - Response state is NOT ok : " + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public KnowledgeDoc importKnowledgeDoc(String token, String fileCode) {
        JSONObject payload = new JSONObject();
        payload.put("fileCode", fileCode);
        Packet packet = new Packet(AIGCAction.ImportKnowledgeDoc.name, payload);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 3 * 60 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#importKnowledgeDoc - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#importKnowledgeDoc - Response state is NOT ok : " + Packet.extractCode(responsePacket));
            return null;
        }

        return new KnowledgeDoc(Packet.extractDataPayload(responsePacket));
    }

    public KnowledgeDoc removeKnowledgeDoc(String token, String fileCode) {
        JSONObject payload = new JSONObject();
        payload.put("fileCode", fileCode);
        Packet packet = new Packet(AIGCAction.RemoveKnowledgeDoc.name, payload);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#removeKnowledgeDoc - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#removeKnowledgeDoc - Response state is NOT ok : " + Packet.extractCode(responsePacket));
            return null;
        }

        return new KnowledgeDoc(Packet.extractDataPayload(responsePacket));
    }

    public boolean evaluate(long sn, int scores) {
        JSONObject data = new JSONObject();
        data.put("sn", sn);
        data.put("scores", scores);
        Packet packet = new Packet(AIGCAction.Evaluate.name, data);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, packet.toDialect());
        if (null == response) {
            Logger.w(Manager.class, "#evaluate - Response is null");
            return false;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(Manager.class, "#evaluate - Response state code is NOT Ok - " + Packet.extractCode(responsePacket));
            return false;
        }

        return true;
    }

    public AIGCChannel requestChannel(String token, String participant) {
        JSONObject data = new JSONObject();
        data.put("token", token);
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

    public boolean keepAliveChannel(String channelCode) {
        JSONObject data = new JSONObject();
        data.put("code", channelCode);
        Packet packet = new Packet(AIGCAction.KeepAliveChannel.name, data);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, packet.toDialect());
        if (null == response) {
            Logger.w(Manager.class, "#keepAliveChannel - Response is null, code : " + channelCode);
            return false;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(Manager.class, "#keepAliveChannel - Response state code is NOT Ok : " + channelCode +
                    " - " + Packet.extractCode(responsePacket));
            return false;
        }

        return true;
    }

    public AIGCChatRecord chat(String token, String channelCode, String pattern, String content, String unit,
                               int histories, JSONArray records) {
        JSONObject data = new JSONObject();
        data.put("token", token);
        data.put("code", channelCode);
        data.put("pattern", pattern);
        data.put("content", content);
        data.put("histories", histories);
        if (null != unit) {
            data.put("unit", unit);
        }
        if (null != records) {
            data.put("records", records);
        }

        Packet packet = new Packet(AIGCAction.Chat.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 90 * 1000);
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
        if (null == record.query) {
            record.query = content;
        }
        return record;
    }

    /**
     * 增强型对话。
     *
     * @param token
     * @param channelCode
     * @oaram pattern
     * @param content
     * @param records
     * @param temperature
     * @param topP
     * @param repetitionPenalty
     * @return
     */
    public AIGCConversationResponse conversation(String token, String channelCode, String pattern,
                                                 String content, JSONArray records,
                                                 float temperature, float topP, float repetitionPenalty) {
        JSONObject data = new JSONObject();
        data.put("token", token);
        data.put("code", channelCode);
        data.put("pattern", pattern);
        data.put("content", content);
        if (temperature >= 0.0 && temperature <= 1.0) {
            data.put("temperature", temperature);
        }
        if (topP >= 0.0 && topP <= 1.0) {
            data.put("topP", topP);
        }
        if (repetitionPenalty >= 0.0) {
            data.put("repetitionPenalty", repetitionPenalty);
        }
        if (null != records) {
            data.put("records", records);
        }

        Packet packet = new Packet(AIGCAction.Conversation.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 120 * 1000);
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

    public JSONObject querySearchResults(String token) {
        Packet packet = new Packet(AIGCAction.GetSearchResults.name, new JSONObject());
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(Manager.class, "#querySearchResults - Response is null");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(Manager.class, "#querySearchResults - Response state code is NOT Ok - "
                    + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public SentimentResult sentimentAnalysis(String text) {
        JSONObject data = new JSONObject();
        data.put("text", text);
        Packet packet = new Packet(AIGCAction.Sentiment.name, data);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, packet.toDialect(), 90 * 1000);
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

    public String generateSummarization(String text) {
        JSONObject data = new JSONObject();
        data.put("text", text);
        Packet packet = new Packet(AIGCAction.Summarization.name, data);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, packet.toDialect(), 90 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#generateSummarization - Response is null");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(Manager.class, "#generateSummarization - Response state code : "
                    + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket).getString("summarization");
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

    public JSONObject handleChartData(String token, JSONObject data) {
        Packet packet = new Packet(AIGCAction.ChartData.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(this.getClass(), "#handleChartData - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#handleChartData - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public JSONObject getPrompts(String token) {
        Packet packet = new Packet(AIGCAction.GetPrompts.name, new JSONObject());
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(this.getClass(), "#getPrompts - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#getPrompts - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public boolean addPrompts(String token, List<Prompt> promptList, List<Long> contactIdList) {
        JSONObject data = new JSONObject();
        data.put("action", "add");

        JSONArray promptArray = new JSONArray();
        for (Prompt prompt : promptList) {
            promptArray.put(prompt.toJSON());
        }
        data.put("prompts", promptArray);

        if (null != contactIdList) {
            JSONArray contactIdArray = new JSONArray();
            for (long contactId : contactIdList) {
                contactIdArray.put(contactId);
            }
            data.put("contactIds", contactIdArray);
        }

        Packet packet = new Packet(AIGCAction.SetPrompts.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(this.getClass(), "#addPrompts - No response");
            return false;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#addPrompts - Response state is " + Packet.extractCode(responsePacket));
            return false;
        }

        return true;
    }

    public boolean removePrompts(String token, List<Long> promptIdList) {
        JSONObject data = new JSONObject();
        data.put("action", "remove");

        JSONArray idArray = new JSONArray();
        for (long id : promptIdList) {
            idArray.put(id);
        }
        data.put("idList", idArray);

        Packet packet = new Packet(AIGCAction.SetPrompts.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(this.getClass(), "#removePrompts - No response");
            return false;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#removePrompts - Response state is " + Packet.extractCode(responsePacket));
            return false;
        }

        return true;
    }

    public boolean updatePrompt(String token, Prompt prompt) {
        JSONObject data = new JSONObject();
        data.put("action", "update");
        data.put("id", prompt.id);
        data.put("act", prompt.act);
        data.put("prompt", prompt.prompt);

        Packet packet = new Packet(AIGCAction.SetPrompts.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(this.getClass(), "#updatePrompt - No response");
            return false;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#updatePrompt - Response state is " + Packet.extractCode(responsePacket));
            return false;
        }

        return true;
    }

    public JSONObject submitEvent(String token, Event event) {
        Packet packet = new Packet(AIGCAction.SubmitEvent.name, event.toJSON());
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(this.getClass(), "#submitEvent - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#submitEvent - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public JSONObject preInfer(String token, String content) {
        JSONObject data = new JSONObject();
        data.put("content", content);

        Packet packet = new Packet(AIGCAction.PreInfer.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(this.getClass(), "#preInfer - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#preInfer - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
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

        // 回调 App 的 onTick
        App.getInstance().onTick(now);
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


    public class ContactToken {

        public final AuthToken authToken;

        public final Contact contact;

        public final long timestamp = System.currentTimeMillis();

        protected ContactToken(AuthToken authToken, Contact contact) {
            this.authToken = authToken;
            this.contact = contact;
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("token", this.authToken.toJSON());
            json.put("contact", this.contact.toJSON());
            return json;
        }
    }
}
