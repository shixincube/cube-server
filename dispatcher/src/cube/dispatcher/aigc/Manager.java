/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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
import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.*;
import cube.aigc.attachment.ui.Event;
import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.Report;
import cube.aigc.psychology.ScaleReport;
import cube.aigc.psychology.composition.AnswerSheet;
import cube.aigc.psychology.composition.Scale;
import cube.aigc.psychology.composition.ScaleResult;
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
import cube.dispatcher.cv.handler.MakeBarCode;
import cube.dispatcher.util.Tickable;
import cube.util.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
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
     * Key：操作序号。
     */
    private Map<Long, ObjectDetectionFuture> objectDetectionFutureMap;

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
        this.objectDetectionFutureMap = new ConcurrentHashMap<>();
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

    public Performer getPerformer() {
        return this.performer;
    }

    private void setupHandler() {
        HttpServer httpServer = this.performer.getHttpServer();

        httpServer.addContextHandler(new Segmentation());
        httpServer.addContextHandler(new Channel());
        httpServer.addContextHandler(new StopProcessing());
        httpServer.addContextHandler(new Chat());
        httpServer.addContextHandler(new Summarization());
        httpServer.addContextHandler(new SemanticSearch());
        httpServer.addContextHandler(new ObjectDetection());
        httpServer.addContextHandler(new AutomaticSpeechRecognition());
        httpServer.addContextHandler(new Conversation());
        httpServer.addContextHandler(new KnowledgeQA());
        httpServer.addContextHandler(new KnowledgeProfiles());
        httpServer.addContextHandler(new KnowledgeInfos());
        httpServer.addContextHandler(new NewKnowledgeBase());
        httpServer.addContextHandler(new DeleteKnowledgeBase());
        httpServer.addContextHandler(new UpdateKnowledgeBase());
        httpServer.addContextHandler(new KnowledgeDocs());
        httpServer.addContextHandler(new ImportKnowledgeDoc());
        httpServer.addContextHandler(new RemoveKnowledgeDoc());
        httpServer.addContextHandler(new ResetKnowledgeStore());
        httpServer.addContextHandler(new KnowledgeSegments());
        httpServer.addContextHandler(new KnowledgeBackup());
        httpServer.addContextHandler(new KnowledgeArticles());
        httpServer.addContextHandler(new AppendKnowledgeArticle());
        httpServer.addContextHandler(new RemoveKnowledgeArticle());
        httpServer.addContextHandler(new ActivateKnowledgeArticle());
        httpServer.addContextHandler(new DeactivateKnowledgeArticle());
        httpServer.addContextHandler(new QueryAllArticleCategories());
        httpServer.addContextHandler(new SearchResults());
        httpServer.addContextHandler(new ContextInference());
        httpServer.addContextHandler(new ChartData());
        httpServer.addContextHandler(new Prompts());
        httpServer.addContextHandler(new SubmitEvent());
        httpServer.addContextHandler(new QueryAppEvents());
        httpServer.addContextHandler(new QueryUsages());
        httpServer.addContextHandler(new ChatHistory());
        httpServer.addContextHandler(new PublicOpinionData());
        httpServer.addContextHandler(new InferByModule());
        httpServer.addContextHandler(new PreInfer());

        httpServer.addContextHandler(new PsychologyReports());
        httpServer.addContextHandler(new CheckPsychology());
        httpServer.addContextHandler(new PsychologyBenchmark());
        httpServer.addContextHandler(new PsychologyStopping());
        httpServer.addContextHandler(new PsychologyReportParts());
        httpServer.addContextHandler(new PsychologyScales());
        httpServer.addContextHandler(new PsychologyScaleOperation());
        httpServer.addContextHandler(new PsychologyConversation());
        httpServer.addContextHandler(new PsychologyPaintings());
        httpServer.addContextHandler(new PaintingLabels());
        httpServer.addContextHandler(new PsychologyPaintingReportState());

        httpServer.addContextHandler(new cube.dispatcher.aigc.handler.app.Session());
        httpServer.addContextHandler(new cube.dispatcher.aigc.handler.app.Verify());
        httpServer.addContextHandler(new cube.dispatcher.aigc.handler.app.Config());
        httpServer.addContextHandler(new cube.dispatcher.aigc.handler.app.Change());
        httpServer.addContextHandler(new cube.dispatcher.aigc.handler.app.Chat());
        httpServer.addContextHandler(new cube.dispatcher.aigc.handler.app.Evaluate());
        httpServer.addContextHandler(new cube.dispatcher.aigc.handler.app.KeepAlive());
        httpServer.addContextHandler(new cube.dispatcher.aigc.handler.app.Inject());

        httpServer.addContextHandler(new MakeBarCode());
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

    public ContactToken checkOrInjectContactToken(String phoneNumber, String userName) {
        JSONObject data = new JSONObject();
        data.put("phone", phoneNumber);
        if (null != userName) {
            data.put("name", userName);
        }

        Packet packet = new Packet(AIGCAction.InjectOrGetToken.name, data);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, packet.toDialect());
        if (null == response) {
            Logger.w(Manager.class, "#checkOrInjectContactToken - Response is null : " + phoneNumber);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#checkOrInjectContactToken - Response state is NOT ok : "
                    + Packet.extractCode(responsePacket));
            return null;
        }

        JSONObject responseJson = Packet.extractDataPayload(responsePacket);
        AuthToken authToken = new AuthToken(responseJson.getJSONObject("token"));
        Contact contact = new Contact(responseJson.getJSONObject("contact"));
        return new ContactToken(authToken, contact);
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

    public ModelConfig getModelConfigByModel(ConfigInfo configInfo, String model) {
        for (ModelConfig config : configInfo.models) {
            if (config.getModel().equalsIgnoreCase(model)) {
                return config;
            }
        }

        return null;
    }

    public ModelConfig getModelConfigByName(ConfigInfo configInfo, String name) {
        for (ModelConfig config : configInfo.models) {
            if (config.getName().equalsIgnoreCase(name)) {
                return config;
            }
        }

        return null;
    }

    public KnowledgeProfile getKnowledgeProfile(String token) {
        Packet packet = new Packet(AIGCAction.GetKnowledgeProfile.name, new JSONObject());
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
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

    public KnowledgeProfile updateKnowledgeProfile(String token, long contactId,
                                                   int state, long maxSize, KnowledgeScope scope) {
        JSONObject data = new JSONObject();
        data.put("contactId", contactId);
        if (-1 != state) {
            data.put("state", state);
        }
        if (-1 != maxSize) {
            data.put("maxSize", maxSize);
        }
        if (null != scope) {
            data.put("scope", scope.name);
        }

        Packet packet = new Packet(AIGCAction.UpdateKnowledgeProfile.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(Manager.class, "#updateKnowledgeProfile - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#updateKnowledgeProfile - Response state is NOT ok : " + Packet.extractCode(responsePacket));
            return null;
        }

        return new KnowledgeProfile(Packet.extractDataPayload(responsePacket));
    }

    public JSONObject getKnowledgeFramework(String token) {
        Packet packet = new Packet(AIGCAction.GetKnowledgeFramework.name, new JSONObject());
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#getKnowledgeFramework - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#getKnowledgeFramework - Response state is NOT ok : " + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public KnowledgeBaseInfo newKnowledgeBase(String token, String baseName, String displayName, String category) {
        JSONObject payload = new JSONObject();
        payload.put("name", baseName);
        payload.put("displayName", displayName);
        if (null != category && category.length() > 0) {
            payload.put("category", category);
        }
        else {
            payload.put("category", displayName);
        }
        Packet packet = new Packet(AIGCAction.NewKnowledgeBase.name, payload);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#newKnowledgeBase - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#newKnowledgeBase - Response state is NOT ok : " + Packet.extractCode(responsePacket));
            return null;
        }

        return new KnowledgeBaseInfo(Packet.extractDataPayload(responsePacket));
    }

    public KnowledgeBaseInfo deleteKnowledgeBase(String token, String baseName) {
        if (null == baseName) {
            return null;
        }

        Logger.d(this.getClass(), "#deleteKnowledgeBase - " + baseName + " - " + token);

        JSONObject payload = new JSONObject();
        payload.put("name", baseName);
        Packet packet = new Packet(AIGCAction.DeleteKnowledgeBase.name, payload);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#deleteKnowledgeBase - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#deleteKnowledgeBase - Response state is NOT ok : " + Packet.extractCode(responsePacket));
            return null;
        }

        return new KnowledgeBaseInfo(Packet.extractDataPayload(responsePacket));
    }

    public KnowledgeBaseInfo updateKnowledgeBase(String token, KnowledgeBaseInfo info) {
        JSONObject payload = new JSONObject();
        payload.put("info", info.toJSON());
        Packet packet = new Packet(AIGCAction.UpdateKnowledgeBase.name, payload);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#updateKnowledgeBase - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#updateKnowledgeBase - Response state is NOT ok : " + Packet.extractCode(responsePacket));
            return null;
        }

        return new KnowledgeBaseInfo(Packet.extractDataPayload(responsePacket));
    }

    public JSONObject getKnowledgeDocs(String token, String baseName) {
        Logger.d(this.getClass(), "#getKnowledgeDocs - " + baseName + " - " + token);

        JSONObject payload = new JSONObject();
        payload.put("base", baseName);
        Packet packet = new Packet(AIGCAction.ListKnowledgeDocs.name, payload);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#getKnowledgeDocs - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#getKnowledgeDocs - Response state is NOT ok : " + Packet.extractCode(responsePacket));
            return null;
        }

        try {
            JSONObject data = Packet.extractDataPayload(responsePacket);
            if (data.has("page")) {
                // 有分页
                int page = data.getInt("page");
                int total = data.getInt("total");
                JSONArray list = data.getJSONArray("list");
                while (list.length() < total) {
                    page += 1;

                    JSONObject packetPayload = new JSONObject();
                    packetPayload.put("base", baseName);
                    packetPayload.put("page", page);
                    packet = new Packet(AIGCAction.ListKnowledgeDocs.name, packetPayload);
                    request = packet.toDialect();
                    request.addParam("token", token);
                    response = this.performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
                    if (null == response) {
                        Logger.w(Manager.class, "#getKnowledgeDocs - Response is null : " + token);
                        break;
                    }

                    responsePacket = new Packet(response);
                    if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
                        Logger.d(Manager.class, "#getKnowledgeDocs - Response state is NOT ok : "
                                + Packet.extractCode(responsePacket));
                        break;
                    }

                    JSONObject nextData = Packet.extractDataPayload(responsePacket);
                    JSONArray nextList = nextData.getJSONArray("list");
                    if (nextList.length() == 0) {
                        Logger.d(Manager.class, "#getKnowledgeDocs - List is empty: "
                                + Packet.extractCode(responsePacket));
                        break;
                    }

                    for (int i = 0; i < nextList.length(); ++i) {
                        list.put(nextList.getJSONObject(i));
                    }
                }

                JSONObject result = new JSONObject();
                result.put("total", list.length());
                result.put("list", list);
                return result;
            }
            else {
                return data;
            }
        } catch (Exception e) {
            Logger.e(Manager.class, "#getKnowledgeDocs - Error", e);
            return null;
        }
    }

    public KnowledgeDoc importKnowledgeDoc(String token, String baseName, String fileCode, String splitter) {
        JSONObject payload = new JSONObject();
        payload.put("base", baseName);
        payload.put("fileCode", fileCode);
        payload.put("splitter", splitter);
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

    public KnowledgeProgress importKnowledgeDocs(String token, String baseName, JSONArray fileCodeArray, String splitter) {
        Logger.d(this.getClass(), "#importKnowledgeDocs - " + baseName + " - " + token);

        JSONObject payload = new JSONObject();
        payload.put("base", baseName);
        payload.put("fileCodeList", fileCodeArray);
        payload.put("splitter", splitter);
        Packet packet = new Packet(AIGCAction.ImportKnowledgeDoc.name, payload);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#importKnowledgeDocs - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#importKnowledgeDocs - Response state is NOT ok : " + Packet.extractCode(responsePacket));
            return null;
        }

        return new KnowledgeProgress(Packet.extractDataPayload(responsePacket));
    }

    public KnowledgeDoc removeKnowledgeDoc(String token, String baseName, String fileCode) {
        JSONObject payload = new JSONObject();
        payload.put("base", baseName);
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
            Logger.d(Manager.class, "#removeKnowledgeDoc - Response state is NOT ok : "
                    + Packet.extractCode(responsePacket));
            return null;
        }

        return new KnowledgeDoc(Packet.extractDataPayload(responsePacket));
    }

    public KnowledgeProgress removeKnowledgeDocs(String token, String baseName, JSONArray fileCodeArray) {
        Logger.d(this.getClass(), "#removeKnowledgeDocs - " + baseName + " - " + token);

        JSONObject payload = new JSONObject();
        payload.put("base", baseName);
        payload.put("fileCodeList", fileCodeArray);
        Packet packet = new Packet(AIGCAction.RemoveKnowledgeDoc.name, payload);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#removeKnowledgeDocs - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#removeKnowledgeDocs - Response state is NOT ok : " + Packet.extractCode(responsePacket));
            return null;
        }

        return new KnowledgeProgress(Packet.extractDataPayload(responsePacket));
    }

    public JSONObject getKnowledgeSegments(String token, String baseName, long docId, int start, int end) {
        JSONObject payload = new JSONObject();
        payload.put("base", baseName);
        payload.put("docId", docId);
        payload.put("start", start);
        payload.put("end", end);
        Packet packet = new Packet(AIGCAction.GetKnowledgeSegments.name, payload);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#getKnowledgeDocSegments - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#getKnowledgeDocSegments - Response state is NOT ok : " + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public KnowledgeProgress getKnowledgeProgress(String token, String baseName, long sn) {
        if (null == baseName) {
            return null;
        }

        JSONObject payload = new JSONObject();
        payload.put("base", baseName);
        payload.put("sn", sn);
        Packet packet = new Packet(AIGCAction.GetKnowledgeProgress.name, payload);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#getKnowledgeProgress - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#getKnowledgeProgress - Response state is NOT ok : "
                    + Packet.extractCode(responsePacket));
            return null;
        }

        return new KnowledgeProgress(Packet.extractDataPayload(responsePacket));
    }

    public ResetKnowledgeProgress getResetKnowledgeProgress(String token, long sn, String baseName) {
        if (null == baseName) {
            return null;
        }

        JSONObject payload = new JSONObject();
        payload.put("sn", sn);
        payload.put("base", baseName);
        Packet packet = new Packet(AIGCAction.GetResetKnowledgeProgress.name, payload);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#getResetKnowledgeProgress - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#getResetKnowledgeProgress - Response state is NOT ok : "
                    + Packet.extractCode(responsePacket));
            return null;
        }

        return new ResetKnowledgeProgress(Packet.extractDataPayload(responsePacket));
    }

    public ResetKnowledgeProgress resetKnowledgeStore(String token, String baseName, boolean backup) {
        if (null == baseName) {
            return null;
        }

        Logger.d(this.getClass(), "#resetKnowledgeStore - " + baseName + " - " + token);

        JSONObject payload = new JSONObject();
        payload.put("base", baseName);
        payload.put("backup", backup);
        Packet packet = new Packet(AIGCAction.ResetKnowledgeStore.name, payload);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#resetKnowledgeStore - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#resetKnowledgeStore - Response state is NOT ok : "
                    + Packet.extractCode(responsePacket));
            return null;
        }

        return new ResetKnowledgeProgress(Packet.extractDataPayload(responsePacket));
    }

    public JSONObject getBackupKnowledgeStores(String token, String baseName) {
        JSONObject payload = new JSONObject();
        payload.put("base", baseName);
        Packet packet = new Packet(AIGCAction.GetBackupKnowledgeStores.name, payload);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#getBackupKnowledgeStores - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#getBackupKnowledgeStores - Response state is NOT ok : "
                    + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public JSONObject getKnowledgeArticle(String token, long articleId) {
        JSONObject param = new JSONObject();
        param.put("articleId", articleId);
        Packet packet = new Packet(AIGCAction.ListKnowledgeArticles.name, param);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#getKnowledgeArticle - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#getKnowledgeArticle - Response state is NOT ok : " + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public JSONObject getKnowledgeArticles(String token, long startTime, long endTime, boolean activated) {
        JSONObject param = new JSONObject();
        param.put("start", startTime);
        param.put("end", endTime);
        param.put("activated", activated);
        Packet packet = new Packet(AIGCAction.ListKnowledgeArticles.name, param);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#getKnowledgeArticles - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#getKnowledgeArticles - Response state is NOT ok : " + Packet.extractCode(responsePacket));
            return null;
        }

        try {
            JSONObject data = Packet.extractDataPayload(responsePacket);
            if (data.has("page")) {
                // 有分页
                int total = data.getInt("total");
                JSONArray list = data.getJSONArray("list");
                int page = data.getInt("page");
                while (list.length() < total) {
                    page += 1;

                    JSONObject packetPayload = new JSONObject();
                    packetPayload.put("page", page);
                    packetPayload.put("start", startTime);
                    packetPayload.put("end", endTime);
                    packet = new Packet(AIGCAction.ListKnowledgeArticles.name, packetPayload);
                    request = packet.toDialect();
                    request.addParam("token", token);
                    response = this.performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
                    if (null == response) {
                        Logger.w(Manager.class, "#getKnowledgeDocs - Response is null : " + token);
                        return null;
                    }

                    responsePacket = new Packet(response);
                    if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
                        Logger.d(Manager.class, "#getKnowledgeDocs - Response state is NOT ok : "
                                + Packet.extractCode(responsePacket));
                        return null;
                    }

                    JSONObject nextData = Packet.extractDataPayload(responsePacket);
                    JSONArray nextList = nextData.getJSONArray("list");
                    for (int i = 0; i < nextList.length(); ++i) {
                        list.put(nextList.getJSONObject(i));
                    }
                }

                JSONObject result = new JSONObject();
                result.put("total", total);
                result.put("list", list);
                return result;
            }
            else {
                return data;
            }
        } catch (Exception e) {
            Logger.e(Manager.class, "#getKnowledgeDocs - Error", e);
            return null;
        }
    }

    public KnowledgeArticle updateKnowledgeArticle(String token, KnowledgeArticle article) {
        Packet packet = new Packet(AIGCAction.UpdateKnowledgeArticle.name, article.toJSON());
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#updateKnowledgeArticle - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#updateKnowledgeArticle - Response state is NOT ok : " + Packet.extractCode(responsePacket));
            return null;
        }

        return new KnowledgeArticle(Packet.extractDataPayload(responsePacket));
    }

    public KnowledgeArticle appendKnowledgeArticle(String token, KnowledgeArticle article) {
        Packet packet = new Packet(AIGCAction.AppendKnowledgeArticle.name, article.toJSON());
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#appendKnowledgeArticle - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#appendKnowledgeArticle - Response state is NOT ok : " + Packet.extractCode(responsePacket));
            return null;
        }

        return new KnowledgeArticle(Packet.extractDataPayload(responsePacket));
    }

    public JSONObject removeKnowledgeArticle(String token, JSONArray idList) {
        JSONObject payload = new JSONObject();
        payload.put("ids", idList);
        Packet packet = new Packet(AIGCAction.RemoveKnowledgeArticle.name, payload);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#removeKnowledgeArticle - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#removeKnowledgeArticle - Response state is NOT ok : " + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public List<KnowledgeArticle> activateKnowledgeArticle(String token, JSONArray idList) {
        List<KnowledgeArticle> result = new ArrayList<>();

        JSONObject payload = new JSONObject();
        payload.put("ids", idList);
        Packet packet = new Packet(AIGCAction.ActivateKnowledgeArticle.name, payload);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 2 * 60 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#activateKnowledgeArticle - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#activateKnowledgeArticle - Response state is NOT ok : " + Packet.extractCode(responsePacket));
            return null;
        }

        JSONArray responseList = Packet.extractDataPayload(responsePacket).getJSONArray("articles");
        for (int i = 0; i < responseList.length(); ++i) {
            KnowledgeArticle article = new KnowledgeArticle(responseList.getJSONObject(i));
            result.add(article);
        }

        return result;
    }

    public List<KnowledgeArticle> deactivateKnowledgeArticle(String token, JSONArray idList) {
        List<KnowledgeArticle> result = new ArrayList<>();

        JSONObject payload = new JSONObject();
        payload.put("ids", idList);
        Packet packet = new Packet(AIGCAction.DeactivateKnowledgeArticle.name, payload);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#deactivateKnowledgeArticle - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#deactivateKnowledgeArticle - Response state is NOT ok : " + Packet.extractCode(responsePacket));
            return null;
        }

        JSONArray responseList = Packet.extractDataPayload(responsePacket).getJSONArray("articles");
        for (int i = 0; i < responseList.length(); ++i) {
            KnowledgeArticle article = new KnowledgeArticle(responseList.getJSONObject(i));
            result.add(article);
        }

        return result;
    }

    public JSONObject queryAllArticleCategories(String token) {
        Packet packet = new Packet(AIGCAction.QueryAllArticleCategories.name, new JSONObject());
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#queryAllArticleCategories - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#queryAllArticleCategories - Response state is NOT Ok : "
                    + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public boolean evaluate(String token, long sn, int feedback) {
        if (feedback < 0) {
            return false;
        }

        JSONObject data = new JSONObject();
        data.put("sn", sn);
        data.put("feedback", feedback);
        Packet packet = new Packet(AIGCAction.Evaluate.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request);
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

    public boolean addAppEvent(String token, AppEvent appEvent) {
        Packet packet = new Packet(AIGCAction.AddAppEvent.name, appEvent.toJSON());
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(Manager.class, "#addAppEvent - Response is null");
            return false;
        }

        Packet responsePacket = new Packet(response);
        return Packet.extractCode(responsePacket) == AIGCStateCode.Ok.code;
    }

    public JSONObject queryAppEvents(String token, long contactId, String event, long start, long end,
                                     int pageIndex, int pageSize) {
        JSONObject requestData = new JSONObject();
        requestData.put("contactId", contactId);
        requestData.put("event", event);
        requestData.put("start", start);
        requestData.put("end", end);
        requestData.put("page", pageIndex);
        requestData.put("size", pageSize);

        Packet packet = new Packet(AIGCAction.QueryAppEvent.name, requestData);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#queryAppEvents - Response is null");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(Manager.class, "#queryAppEvents - Response state code is NOT Ok - "
                    + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public JSONObject queryUsages(String token, long contactId) {
        JSONObject requestData = new JSONObject();
        requestData.put("contactId", contactId);

        Packet packet = new Packet(AIGCAction.QueryUsages.name, requestData);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#queryUsages - Response is null");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(Manager.class, "#queryUsages - Response state code is NOT Ok - "
                    + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public JSONObject queryChatHistory(String token, String channelCode, long contactId, int feedback, long start, long end) {
        JSONObject requestData = new JSONObject();
        if (null != channelCode) {
            requestData.put("channel", channelCode);
        }
        requestData.put("contactId", contactId);
        requestData.put("feedback", feedback);
        requestData.put("start", start);
        requestData.put("end", end);

        Packet packet = new Packet(AIGCAction.QueryChatHistory.name, requestData);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#queryChatHistory - Response is null");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(Manager.class, "#queryChatHistory - Response state code is NOT Ok - "
                    + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
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

    public AIGCChannel stopProcessing(String token, String channelCode) {
        JSONObject data = new JSONObject();
        data.put("code", channelCode);

        Packet packet = new Packet(AIGCAction.StopChannel.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(Manager.class, "#stopProcessing - Response is null : " + channelCode);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(Manager.class, "#stopProcessing - Response state code is NOT Ok: " + channelCode +
                    " - " + Packet.extractCode(responsePacket));
            return null;
        }

        return new AIGCChannel(Packet.extractDataPayload(responsePacket));
    }

    public JSONObject getChannel(String token, String channelCode) {
        JSONObject data = new JSONObject();
        data.put("code", channelCode);

        Packet packet = new Packet(AIGCAction.GetChannelInfo.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(Manager.class, "#getChannel - Response is null : " + channelCode);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(Manager.class, "#getChannel - Response state code is NOT Ok: " + channelCode +
                    " - " + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
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

    /**
     * 互动对话。
     *
     * @param token
     * @param channelCode
     * @param pattern
     * @param content
     * @param unit
     * @param option
     * @param histories
     * @param records
     * @param recordable
     * @param searchable
     * @param networking
     * @param categories
     * @param searchTopK
     * @param searchFetchK
     * @return
     */
    public ChatFuture chat(String token, String channelCode, String pattern, String content, String unit,
                           GenerativeOption option, int histories, JSONArray records,
                           boolean recordable, boolean searchable, boolean networking,
                           JSONArray categories, int searchTopK, int searchFetchK) {
        JSONObject data = new JSONObject();
        data.put("token", token);
        data.put("code", channelCode);
        data.put("pattern", pattern);
        data.put("content", content);
        data.put("option", option.toJSON());
        data.put("histories", histories);
        if (null != unit) {
            data.put("unit", unit);
        }
        if (null != records) {
            data.put("records", records);
        }
        if (null != categories) {
            data.put("categories", categories);
        }
        data.put("recordable", recordable);
        data.put("searchable", searchable);
        data.put("networking", networking);
        data.put("searchTopK", searchTopK);
        data.put("searchFetchK", searchFetchK);

        Packet responsePacket = null;

        if (ModelConfig.BAIZE_NEXT_UNIT.equalsIgnoreCase(unit)) {
            Packet packet = new Packet(AIGCAction.Conversation.name, data);
            ActionDialect request = packet.toDialect();
            request.addParam("token", token);
            ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
            if (null == response) {
                Logger.w(Manager.class, "#chat - Response (conversation) is null - " + channelCode);
                return null;
            }

            responsePacket = new Packet(response);
        }
        else {
            Packet packet = new Packet(AIGCAction.Chat.name, data);
            ActionDialect request = packet.toDialect();
            request.addParam("token", token);
            ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 4 * 60 * 1000);
            if (null == response) {
                Logger.w(Manager.class, "#chat - Response is null - " + channelCode);
                return null;
            }

            responsePacket = new Packet(response);
        }

        if (Packet.extractCode(responsePacket) == AIGCStateCode.Processing.code) {
            AIGCChannel channel = new AIGCChannel(Packet.extractDataPayload(responsePacket));
            ChatFuture future = new ChatFuture(channel);
            future.end = false;
            return future;
        }
        else if (Packet.extractCode(responsePacket) == AIGCStateCode.Ok.code) {
            ChatFuture future = null;
            if (Consts.PATTERN_CHAT.equals(pattern)) {
                JSONObject responseData = Packet.extractDataPayload(responsePacket);
                if (responseData.has("processing") && responseData.has("channel")) {
                    // 来自 conversation 的结果
                    future = new ChatFuture(new AIGCChannel(responseData.getJSONObject("channel")));
                    future.end = false;
                }
                else {
                    GenerativeRecord record = new GenerativeRecord(responseData);
                    if (null == record.query) {
                        record.query = content;
                    }
                    future = new ChatFuture(record);
                    future.end = true;
                }
            }
            else if (Consts.PATTERN_KNOWLEDGE.equals(pattern)) {
                KnowledgeQAResult result = new KnowledgeQAResult(Packet.extractDataPayload(responsePacket));
                future = new ChatFuture(result);
                future.end = true;
            }

            return future;
        }
        else {
            Logger.w(Manager.class, "#chat - Response state code is NOT Ok - " + channelCode +
                    " - " + Packet.extractCode(responsePacket));
            return null;
        }
    }

    /**
     * 互动对话。
     *
     * @param token
     * @param channelCode
     * @param pattern
     * @param content
     * @param histories
     * @param records
     * @param option
     * @return
     */
    public long executeConversation(String token, String channelCode, String pattern,
                             String content, int histories, JSONArray records,
                             GenerativeOption option) {
        JSONObject data = new JSONObject();
        data.put("token", token);
        data.put("code", channelCode);
        data.put("pattern", pattern);
        data.put("content", content);
        data.put("option", option.toJSON());
        data.put("histories", histories);
        if (null != records) {
            data.put("records", records);
        }
        data.put("recordable", false);
        data.put("searchable", false);
        data.put("networking", false);
        data.put("searchTopK", 20);
        data.put("searchFetchK", 50);

        Packet packet = new Packet(AIGCAction.Conversation.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#conversation - Response is null - " + channelCode);
            return 0;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(Manager.class, "#conversation - Response state code is NOT Ok - " + channelCode +
                    " - " + Packet.extractCode(responsePacket));
            return 0;
        }

        JSONObject responseData = Packet.extractDataPayload(responsePacket);
        return responseData.getLong("sn");
    }

    public AIGCConversationResponse queryConversation(String token, String channelCode, long sn) {
        JSONObject data = new JSONObject();
        data.put("token", token);
        data.put("code", channelCode);
        data.put("sn", sn);

        Packet packet = new Packet(AIGCAction.QueryConversation.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#queryConversation - Response is null - " + channelCode);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(Manager.class, "#queryConversation - Response state code is NOT Ok - " + channelCode +
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

    public JSONObject getContextInference(String token, long contextId) {
        JSONObject data = new JSONObject();
        data.put("id", contextId);
        Packet packet = new Packet(AIGCAction.GetContextInference.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(Manager.class, "#getContextInference - Response is null");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(Manager.class, "#getContextInference - Response state code is NOT Ok - "
                    + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public KnowledgeQAProgress performKnowledgeQA(String token, String unit, KnowledgeMatchingSchema matchingSchema) {
        JSONObject data = new JSONObject();
        data.put("unit", unit);
        data.put("matchingSchema", matchingSchema.toJSON());
        Packet packet = new Packet(AIGCAction.PerformKnowledgeQA.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(Manager.class, "#performKnowledgeQA - Response is null: " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(Manager.class, "#performKnowledgeQA - Response state code is NOT Ok - "
                    + Packet.extractCode(responsePacket));
            return null;
        }

        return new KnowledgeQAProgress(Packet.extractDataPayload(responsePacket));
    }

    public KnowledgeQAProgress getKnowledgeQAProgress(String token) {
        Packet packet = new Packet(AIGCAction.GetKnowledgeQAProgress.name, new JSONObject());
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(Manager.class, "#getKnowledgeQAProgress - Response is null: " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(Manager.class, "#getKnowledgeQAProgress - Response state code is NOT Ok - "
                    + Packet.extractCode(responsePacket));
            return null;
        }

        return new KnowledgeQAProgress(Packet.extractDataPayload(responsePacket));
    }

    public JSONObject semanticSearch(String query) {
        JSONObject data = new JSONObject();
        data.put("query", query);
        Packet packet = new Packet(AIGCAction.SemanticSearch.name, data);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, packet.toDialect(), 90 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#semanticSearch - Response is null");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(Manager.class, "#semanticSearch - Response state code : "
                    + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
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

    /*public NLTask performNaturalLanguageTask(NLTask task) {
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
    }*/

    public ObjectDetectionFuture objectDetection(String token, String channelCode, JSONArray fileCodeList) {
        long sn = Utils.generateSerialNumber();

        JSONObject data = new JSONObject();
        data.put("sn", sn);
        data.put("code", channelCode);
        data.put("fileCodeList", fileCodeList);
        Packet packet = new Packet(AIGCAction.ObjectDetection.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ObjectDetectionFuture future = new ObjectDetectionFuture(sn, channelCode, fileCodeList);
        this.objectDetectionFutureMap.put(sn, future);

        this.performer.transmit(AIGCCellet.NAME, request);

        return future;
    }

    public ObjectDetectionFuture getObjectDetectionFuture(long sn) {
        return this.objectDetectionFutureMap.get(sn);
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

    public ASRFuture getASRFuture(String fileCode) {
        return this.asrFutureMap.get(fileCode);
    }

    public JSONObject segmentation(String token, String text) {
        JSONObject data = new JSONObject();
        data.put("text", text);
        Packet packet = new Packet(AIGCAction.Segmentation.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(this.getClass(), "#segmentation - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#segmentation - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
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

    public boolean addPrompts(String token, List<PromptRecord> promptRecordList, List<Long> contactIdList) {
        JSONObject data = new JSONObject();
        data.put("action", "add");

        JSONArray promptArray = new JSONArray();
        for (PromptRecord promptRecord : promptRecordList) {
            promptArray.put(promptRecord.toJSON());
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

    public boolean updatePrompt(String token, PromptRecord promptRecord) {
        JSONObject data = new JSONObject();
        data.put("action", "update");
        data.put("id", promptRecord.id);
        data.put("act", promptRecord.act);
        data.put("prompt", promptRecord.prompt);

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

    public JSONObject handlePublicOpinionData(String token, JSONObject data) {
        Packet packet = new Packet(AIGCAction.PublicOpinionData.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(this.getClass(), "#handlePublicOpinionData - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#handlePublicOpinionData - Response state is "
                    + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public JSONObject inferByModule(String token, String moduleName, JSONObject param) {
        JSONObject data = new JSONObject();
        data.put("module", moduleName);
        data.put("param", param);
        Packet packet = new Packet(AIGCAction.InferByModule.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(this.getClass(), "#inferByModule - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#inferByModule - Response state is "
                    + Packet.extractCode(responsePacket));
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

    /**
     * 生成心理测验报告。
     *
     * @param remote
     * @param token
     * @param attribute
     * @param fileCode
     * @param theme
     * @param indicatorTexts
     * @return
     */
    public PaintingReport generatePsychologyReport(String remote, String token, Attribute attribute,
                                                   String fileCode, String theme, int indicatorTexts) {
        JSONObject data = new JSONObject();
        data.put("remote", remote);
        data.put("attribute", attribute.toJSON());
        data.put("fileCode", fileCode);
        data.put("theme", theme);
        data.put("indicators", indicatorTexts);

        Packet packet = new Packet(AIGCAction.GeneratePsychologyReport.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#generatePsychologyReport - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#generatePsychologyReport - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        PaintingReport report = new PaintingReport(Packet.extractDataPayload(responsePacket));
        return report;
    }

    /**
     * 生成心理测验报告。
     *
     * @param remote
     * @param token
     * @param scaleSn
     * @return
     */
    public ScaleReport generatePsychologyReport(String remote, String token, long scaleSn) {
        JSONObject data = new JSONObject();
        data.put("remote", remote);
        data.put("scaleSn", scaleSn);

        Packet packet = new Packet(AIGCAction.GeneratePsychologyReport.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#generatePsychologyReport - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#generatePsychologyReport - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        ScaleReport report = new ScaleReport(Packet.extractDataPayload(responsePacket));
        return report;
    }

    /**
     * 查询心理学报告。
     *
     * @param token
     * @param pageIndex
     * @param pageSize
     * @param descending
     * @return
     */
    public JSONObject getPsychologyReports(String token, int pageIndex, int pageSize, boolean descending) {
        if (pageSize <= 0) {
            return null;
        }

        JSONObject data = new JSONObject();
        data.put("page", pageIndex);
        data.put("size", pageSize);
        data.put("desc", descending);
        data.put("state", 0);
        Packet packet = new Packet(AIGCAction.GetPsychologyReport.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#getPsychologyReports - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#getPsychologyReports - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public JSONObject checkPsychologyPainting(String token, String fileCode) {
        JSONObject data = new JSONObject();
        data.put("fileCode", fileCode);
        Packet packet = new Packet(AIGCAction.CheckPsychologyPainting.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#checkPsychologyPainting - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#checkPsychologyPainting - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public Report getPsychologyReport(String token, long sn, boolean markdown) {
        // 第一步，获取基础数据
        JSONObject data = new JSONObject();
        data.put("sn", sn);
        data.put("texts", false);
        data.put("markdown", false);
        Packet packet = new Packet(AIGCAction.GetPsychologyReport.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#getPsychologyReport - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#getPsychologyReport - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        // 解析报告
        JSONObject reportJson = Packet.extractDataPayload(responsePacket);
        if (reportJson.has("factors") && !reportJson.has("fileLabel")) {
            // 量表报告
            return new ScaleReport(reportJson);
        }

        PaintingReport report = new PaintingReport(reportJson);

        // 第二步，获取文本数据
        data = new JSONObject();
        data.put("sn", sn);
        data.put("texts", true);
        data.put("markdown", false);
        packet = new Packet(AIGCAction.GetPsychologyReport.name, data);
        request = packet.toDialect();
        request.addParam("token", token);

        response = performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#getPsychologyReport - No response");
            return null;
        }

        responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#getPsychologyReport - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        report.extendTextList(Packet.extractDataPayload(responsePacket));

        // 第三步，是否获取 Markdown 数据
        if (markdown) {
            data = new JSONObject();
            data.put("sn", sn);
            data.put("texts", false);
            data.put("markdown", true);
            packet = new Packet(AIGCAction.GetPsychologyReport.name, data);
            request = packet.toDialect();
            request.addParam("token", token);

            response = performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
            if (null == response) {
                Logger.w(this.getClass(), "#getPsychologyReport - No response");
                return null;
            }

            responsePacket = new Packet(response);
            if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
                Logger.w(this.getClass(), "#getPsychologyReport - Response state is " + Packet.extractCode(responsePacket));
                return null;
            }

            report.extendMarkdown(Packet.extractDataPayload(responsePacket));
        }

        return report;
    }

    public JSONObject getPsychologyReportPart(String token, long sn, boolean reportText) {
        JSONObject data = new JSONObject();
        data.put("sn", sn);
        data.put("reportText", reportText);
        Packet packet = new Packet(AIGCAction.GetPsychologyReportPart.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#getPsychologyReportPart - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#getPsychologyReportPart - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public JSONObject stopGeneratingPsychologyReport(String token, long sn) {
        JSONObject data = new JSONObject();
        data.put("sn", sn);
        Packet packet = new Packet(AIGCAction.StopGeneratingPsychologyReport.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#stopGeneratingPsychologyReport - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#stopGeneratingPsychologyReport - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public JSONObject getPsychologyScoreBenchmark(String token, int age) {
        JSONObject data = new JSONObject();
        data.put("age", age);
        Packet packet = new Packet(AIGCAction.GetPsychologyScoreBenchmark.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#getPsychologyScoreBenchmark - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#getPsychologyScoreBenchmark - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }
        return Packet.extractDataPayload(responsePacket);
    }

    public JSONObject listPsychologyScales(String token) {
        Packet packet = new Packet(AIGCAction.ListPsychologyScales.name, new JSONObject());
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#listPsychologyScales - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#listPsychologyScales - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public Scale getPsychologyScale(String token, long sn) {
        JSONObject data = new JSONObject();
        data.put("sn", sn);
        Packet packet = new Packet(AIGCAction.GetPsychologyScale.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#getPsychologyScale - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#getPsychologyScale - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        return new Scale(Packet.extractDataPayload(responsePacket));
    }

    public Scale generatePsychologyScale(String token, String scaleName, String gender, int age) {
        JSONObject data = new JSONObject();
        data.put("name", scaleName);
        data.put("gender", gender);
        data.put("age", age);
        Packet packet = new Packet(AIGCAction.GeneratePsychologyScale.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#generatePsychologyScale - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#generatePsychologyScale - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        return new Scale(Packet.extractDataPayload(responsePacket));
    }

    public ScaleResult submitPsychologyAnswerSheet(String token, AnswerSheet answerSheet) {
        Packet packet = new Packet(AIGCAction.SubmitPsychologyAnswerSheet.name, answerSheet.toJSON());
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#submitPsychologyAnswerSheet - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#submitPsychologyAnswerSheet - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        return new ScaleResult(Packet.extractDataPayload(responsePacket));
    }

    public JSONObject executePsychologyConversation(String token, String channelCode,
                                                    JSONArray reportRelations, String query) {
        JSONObject data = new JSONObject();
        data.put("channelCode", channelCode);
        data.put("relations", reportRelations);
        data.put("query", query);
        Packet packet = new Packet(AIGCAction.PsychologyConversation.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = performer.syncTransmit(AIGCCellet.NAME, request, 90 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#executePsychologyConversation - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#executePsychologyConversation - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public JSONObject getPsychologyPainting(String token, long reportSn) {
        JSONObject data = new JSONObject();
        data.put("sn", reportSn);
        Packet packet = new Packet(AIGCAction.GetPsychologyPainting.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#getPsychologyPainting - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#getPsychologyPainting - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public JSONObject getPaintingLabels(String token, long reportSn) {
        JSONObject data = new JSONObject();
        data.put("sn", reportSn);
        Packet packet = new Packet(AIGCAction.GetPaintingLabel.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 90 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#getPaintingLabels - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#getPaintingLabels - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public boolean submitPaintingLabels(String token, long sn, JSONArray labels) {
        JSONObject data = new JSONObject();
        data.put("sn", sn);
        data.put("labels", labels);

        Packet packet = new Packet(AIGCAction.SetPaintingLabel.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 90 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#submitPaintingLabels - No response");
            return false;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#submitPaintingLabels - Response state is " + Packet.extractCode(responsePacket));
            return false;
        }

        return true;
    }

    public boolean setPaintingReportState(String token, long sn, int state) {
        JSONObject data = new JSONObject();
        data.put("sn", sn);
        data.put("state", state);
        Packet packet = new Packet(AIGCAction.SetPaintingReportState.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.performer.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#setPaintingReportState - No response");
            return false;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#setPaintingReportState - Response state is " + Packet.extractCode(responsePacket));
            return false;
        }

        return true;
    }

    @Override
    public void onTick(long now) {
        if (now - this.lastClearToken > 60 * 60 * 1000) {
            this.validTokenMap.clear();
            this.lastClearToken = now;

            Iterator<Map.Entry<Long, ObjectDetectionFuture>> odIter = this.objectDetectionFutureMap.entrySet().iterator();
            while (odIter.hasNext()) {
                Map.Entry<Long, ObjectDetectionFuture> e = odIter.next();
                if (now - e.getValue().timestamp > 30 * 60 * 1000) {
                    odIter.remove();
                }
            }

            Iterator<Map.Entry<String, ASRFuture>> asrIter = this.asrFutureMap.entrySet().iterator();
            while (asrIter.hasNext()) {
                Map.Entry<String, ASRFuture> e = asrIter.next();
                ASRFuture future = e.getValue();
                if (now - future.timestamp > 30 * 60 * 1000) {
                    asrIter.remove();
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

        if (AIGCAction.ObjectDetection.name.equals(action)) {
            Packet responsePacket = new Packet(actionDialect);
            // 状态码
            int stateCode = Packet.extractCode(responsePacket);
            if (stateCode == AIGCStateCode.Ok.code) {
                JSONObject resultJson = Packet.extractDataPayload(responsePacket);
                JSONArray resultArray = resultJson.getJSONArray("list");
                List<ObjectDetectionResult> resultList = new ArrayList<>();
                for (int i = 0; i < resultArray.length(); ++i) {
                    resultList.add(new ObjectDetectionResult(resultArray.getJSONObject(i)));
                }
                ObjectDetectionFuture future = this.objectDetectionFutureMap.get(resultJson.getLong("sn"));
                if (null != future) {
                    future.resultList = resultList;
                    future.stateCode = stateCode;
                }
                else {
                    Logger.e(this.getClass(), "#onReceived - ObjectDetection result error: " + resultJson.toString());
                }
            }
            else {
                JSONObject resultJson = Packet.extractDataPayload(responsePacket);
                ObjectDetectionFuture future = this.objectDetectionFutureMap.get(resultJson.getLong("sn"));
                if (null != future) {
                    future.stateCode = stateCode;
                }
                else {
                    Logger.e(this.getClass(), "#onReceived - ObjectDetection result error: " + resultJson.toString());
                }
            }
        }
        else if (AIGCAction.AutomaticSpeechRecognition.name.equals(action)) {
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

    public class ChatFuture implements JSONable {

        public boolean end = true;

        public AIGCChannel channel;

        public GenerativeRecord record;

        public KnowledgeQAResult knowledgeResult;

        protected ChatFuture(AIGCChannel channel) {
            this.channel = channel;
        }

        protected ChatFuture(GenerativeRecord record) {
            this.record = record;
        }

        protected ChatFuture(KnowledgeQAResult knowledgeResult) {
            this.knowledgeResult = knowledgeResult;
        }

        @Override
        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            return json;
        }

        @Override
        public JSONObject toCompactJSON() {
            return this.toJSON();
        }
    }

    public class ObjectDetectionFuture implements JSONable {

        protected final long timestamp;

        protected final long sn;

        protected String channelCode;

        protected JSONArray fileCodeList;

        protected List<ObjectDetectionResult> resultList;

        protected int stateCode = AIGCStateCode.Processing.code;

        public ObjectDetectionFuture(long sn, String channelCode, JSONArray fileCodeList) {
            this.sn = sn;
            this.timestamp = System.currentTimeMillis();
            this.channelCode = channelCode;
            this.fileCodeList = fileCodeList;
        }

        @Override
        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("sn", this.sn);
            json.put("channelCode", this.channelCode);
            json.put("timestamp", this.timestamp);
            json.put("stateCode", this.stateCode);

            if (null != this.resultList) {
                JSONArray array = new JSONArray();
                for (ObjectDetectionResult result : this.resultList) {
                    array.put(result.toJSON());
                }
                json.put("resultList", array);
            }
            return json;
        }

        @Override
        public JSONObject toCompactJSON() {
            return this.toJSON();
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
