/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.*;
import cube.aigc.app.ConfigInfo;
import cube.aigc.complex.widget.Event;
import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.Report;
import cube.aigc.psychology.ScaleReport;
import cube.aigc.psychology.algorithm.Attention;
import cube.aigc.psychology.app.UserProfile;
import cube.aigc.psychology.composition.AnswerSheet;
import cube.aigc.psychology.composition.Scale;
import cube.aigc.psychology.composition.ScaleResult;
import cube.aigc.psychology.consultation.ConsultationTheme;
import cube.auth.AuthToken;
import cube.common.JSONable;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.dispatcher.Performer;
import cube.dispatcher.PerformerListener;
import cube.dispatcher.aigc.handler.*;
import cube.dispatcher.aigc.handler.app.App;
import cube.dispatcher.aigc.spi.DispatcherExtension;
import cube.dispatcher.aigc.spi.DispatcherExtensions;
import cube.dispatcher.stream.StreamType;
import cube.dispatcher.util.Tickable;
import cube.util.FileLabels;
import cube.util.FileUtils;
import cube.util.HttpServer;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 接口管理器。
 */
public class Manager implements Tickable, PerformerListener {

    private final static Manager instance = new Manager();

    private volatile Performer performer;

    private volatile boolean running;

    private long generation;

    private static final long TOKEN_CACHE_TTL = 24L * 60 * 60 * 1000;

    private static final long FUTURE_CACHE_TTL = 60L * 60 * 1000;

    private final Set<HttpServer> initializedHttpServers =
            Collections.newSetFromMap(new WeakHashMap<HttpServer, Boolean>());

    /** 按协议封包序号关联任务，避免 reset 后的旧应答完成新任务。 */
    private final Map<Long, PendingRequest> pendingRequests = new ConcurrentHashMap<>();

    // 仅在生命周期锁内访问；完成、替换及过期时同步移除两个索引。
    private final Map<JSONable, PendingRequest> pendingFutures = new IdentityHashMap<>();

    /**
     * 已装载的业务模块网关扩展，按装载顺序。
     */
    private List<DispatcherExtension> dispatcherExtensions;

    private long lastTickTime;

    /**
     * Key：Token code
     */
    private final Map<String, ContactToken> validTokenMap = new ConcurrentHashMap<>();

    /**
     * Key：操作序号。
     */
    private final Map<Long, TextToFileFuture> textToFileFutureMap = new ConcurrentHashMap<>();

    /**
     * Key：查询码
     */
    private final Map<String, SpeechRecognitionFuture> speechRecognitionFutureMap = new ConcurrentHashMap<>();

    /**
     * Key：文件码
     */
    private final Map<String, SpeechEmotionRecognitionFuture> speechEmotionRecognitionFutureMap = new ConcurrentHashMap<>();

    /**
     * Key：查询码
     */
    private final Map<String, SpeechDiarizationFuture> speechDiarizationFutureMap = new ConcurrentHashMap<>();

    /**
     * Key：文件码
     */
    private final Map<String, FacialExpressionRecognitionFuture> facialExpressionRecognitionFutureMap = new ConcurrentHashMap<>();

    public static Manager getInstance() {
        return Manager.instance;
    }

    public synchronized void start(Performer performer) {
        if (null == performer || null == performer.getHttpServer()) {
            throw new IllegalArgumentException("A configured Performer is required");
        }
        if (this.running) {
            if (this.performer != performer) {
                throw new IllegalStateException("Manager is already bound to another Performer");
            }
            return;
        }
        this.performer = performer;
        ++this.generation;
        try {
            HttpServer httpServer = performer.getHttpServer();
            if (!this.initializedHttpServers.contains(httpServer)) {
                this.setupHandler();
                this.initializedHttpServers.add(httpServer);
            }
            App.getInstance().start();
            performer.addTickable(this);
            performer.setListener(AIGCCellet.NAME, this);
            this.lastTickTime = System.currentTimeMillis();
            this.running = true;
        } catch (RuntimeException | Error e) {
            performer.removeListener(AIGCCellet.NAME, this);
            performer.removeTickable(this);
            App.getInstance().stop();
            throw e;
        }
    }

    public synchronized void stop() {
        if (!this.running) {
            return;
        }
        this.running = false;
        ++this.generation;
        try {
            this.performer.removeListener(AIGCCellet.NAME, this);
            this.performer.removeTickable(this);
            App.getInstance().stop();
        } finally {
            for (PendingRequest pending : this.pendingRequests.values()) {
                pending.fail(AIGCStateCode.Cancelled);
            }
            this.pendingRequests.clear();
            this.pendingFutures.clear();
            this.validTokenMap.clear();
            this.textToFileFutureMap.clear();
            this.speechRecognitionFutureMap.clear();
            this.speechEmotionRecognitionFutureMap.clear();
            this.speechDiarizationFutureMap.clear();
            this.facialExpressionRecognitionFutureMap.clear();
        }
    }

    private synchronized Performer activePerformer() {
        return this.running ? this.performer : null;
    }

    public Performer getPerformer() {
        return this.performer;
    }

    /** 不持有生命周期锁等待网络；已经开始的同步请求可以正常结束。 */
    private ActionDialect syncTransmit(String cellet, ActionDialect request) {
        Performer current;
        synchronized (this) {
            if (!this.running) {
                return null;
            }
            current = this.performer;
        }
        return current.syncTransmit(cellet, request);
    }

    private ActionDialect syncTransmit(String cellet, ActionDialect request, long timeout) {
        Performer current;
        synchronized (this) {
            if (!this.running) {
                return null;
            }
            current = this.performer;
        }
        return current.syncTransmit(cellet, request, timeout);
    }

    private <K, F extends JSONable> F submitFuture(Packet packet, String token,
                                                  Map<K, F> cache, K key, F future, boolean reset) {
        final PendingRequest pending;
        final Performer current;
        synchronized (this) {
            if (!this.running || isBlank(token)) {
                return null;
            }
            F previous = cache.get(key);
            if (null != previous) {
                boolean expired = System.currentTimeMillis() - futureTimestamp(previous) >= FUTURE_CACHE_TTL;
                if (!reset && !expired) {
                    return previous;
                }
                this.detachFuture(previous, expired ? AIGCStateCode.Expired : AIGCStateCode.Cancelled);
            }
            current = this.performer;
            pending = new PendingRequest(packet.sn, packet.name, key, cache, future);
            cache.put(key, future);
            this.pendingRequests.put(packet.sn, pending);
            this.pendingFutures.put(future, pending);
        }
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        boolean sent;
        try {
            sent = current.tryTransmit(AIGCCellet.NAME, request);
        } catch (RuntimeException e) {
            Logger.w(Manager.class, "#submitFuture - " + packet.name, e);
            sent = false;
        }
        synchronized (this) {
            if (!sent && this.pendingRequests.get(packet.sn) == pending) {
                pending.fail(AIGCStateCode.Failure);
                this.removePending(pending);
            }
        }
        return future;
    }

    private void removePending(PendingRequest pending) {
        this.pendingRequests.remove(pending.sn, pending);
        this.pendingFutures.remove(pending.future);
    }

    private void detachFuture(JSONable future, AIGCStateCode state) {
        PendingRequest pending = this.pendingFutures.get(future);
        if (null != pending) {
            pending.fail(state);
            this.removePending(pending);
        }
    }

    private static long futureTimestamp(JSONable future) {
        if (future instanceof TextToFileFuture) {
            return ((TextToFileFuture) future).timestamp;
        }
        if (future instanceof SpeechRecognitionFuture) {
            return ((SpeechRecognitionFuture) future).timestamp;
        }
        if (future instanceof SpeechEmotionRecognitionFuture) {
            return ((SpeechEmotionRecognitionFuture) future).timestamp;
        }
        if (future instanceof SpeechDiarizationFuture) {
            return ((SpeechDiarizationFuture) future).timestamp;
        }
        return ((FacialExpressionRecognitionFuture) future).timestamp;
    }

    private static void failFuture(JSONable future, AIGCStateCode state) {
        synchronized (future) {
            if (future instanceof TextToFileFuture) {
                TextToFileFuture f = (TextToFileFuture) future;
                if (f.stateCode == AIGCStateCode.Processing.code) {
                    f.stateCode = state.code;
                }
            }
            else if (future instanceof SpeechRecognitionFuture) {
                SpeechRecognitionFuture f = (SpeechRecognitionFuture) future;
                if (f.stateCode == AIGCStateCode.Processing) {
                    f.stateCode = state;
                }
            }
            else if (future instanceof SpeechEmotionRecognitionFuture) {
                SpeechEmotionRecognitionFuture f = (SpeechEmotionRecognitionFuture) future;
                if (f.stateCode == AIGCStateCode.Processing) {
                    f.stateCode = state;
                }
            }
            else if (future instanceof SpeechDiarizationFuture) {
                SpeechDiarizationFuture f = (SpeechDiarizationFuture) future;
                if (f.stateCode == AIGCStateCode.Processing) {
                    f.stateCode = state;
                }
            }
            else if (future instanceof FacialExpressionRecognitionFuture) {
                FacialExpressionRecognitionFuture f = (FacialExpressionRecognitionFuture) future;
                if (f.stateCode == AIGCStateCode.Processing) {
                    f.stateCode = state;
                }
            }
        }
    }

    private static final class PendingRequest {

        final long sn;
        final String action;
        final Object key;
        final Map<?, ?> cache;
        final JSONable future;

        PendingRequest(long sn, String action, Object key, Map<?, ?> cache, JSONable future) {
            this.sn = sn;
            this.action = action;
            this.key = key;
            this.cache = cache;
            this.future = future;
        }

        boolean isCurrent() {
            return this.cache.get(this.key) == this.future;
        }

        void fail(AIGCStateCode state) {
            failFuture(this.future, state);
        }
    }

    private void registerHandler(HttpServer server, ContextHandler handler) {
        for (ContextHandler existing : server.getContextHandlers()) {
            if (Objects.equals(existing.getContextPath(), handler.getContextPath())) {
                return;
            }
        }
        server.addContextHandler(handler);
    }

    private void setupHandler() {
        HttpServer httpServer = this.performer.getHttpServer();

        this.registerHandler(httpServer, new Static());
        this.registerHandler(httpServer, new InterfaceDocument());

        this.registerHandler(httpServer, new Segmentation());
        this.registerHandler(httpServer, new Channel());
        this.registerHandler(httpServer, new StopProcessing());
        this.registerHandler(httpServer, new Chat());
        this.registerHandler(httpServer, new MultimodalBase());
        this.registerHandler(httpServer, new MultimodalStream());
        this.registerHandler(httpServer, new Summarization());
        this.registerHandler(httpServer, new SemanticSearch());
        this.registerHandler(httpServer, new SpeechEmotionRecognition());
        this.registerHandler(httpServer, new AutomaticSpeechRecognition());
        this.registerHandler(httpServer, new FacialExpressionRecognition());
        this.registerHandler(httpServer, new SpeechDiarization());
        this.registerHandler(httpServer, new SpeechDiarizationOperation());
        this.registerHandler(httpServer, new KnowledgeQA());
        this.registerHandler(httpServer, new KnowledgeProfiles());
        this.registerHandler(httpServer, new KnowledgeInfos());
        this.registerHandler(httpServer, new NewKnowledgeBase());
        this.registerHandler(httpServer, new DeleteKnowledgeBase());
        this.registerHandler(httpServer, new UpdateKnowledgeBase());
        this.registerHandler(httpServer, new KnowledgeDocs());
        this.registerHandler(httpServer, new ImportKnowledgeDoc());
        this.registerHandler(httpServer, new RemoveKnowledgeDoc());
        this.registerHandler(httpServer, new ResetKnowledgeStore());
        this.registerHandler(httpServer, new KnowledgeSegments());
        this.registerHandler(httpServer, new KnowledgeBackup());
        this.registerHandler(httpServer, new KnowledgeArticles());
        this.registerHandler(httpServer, new AppendKnowledgeArticle());
        this.registerHandler(httpServer, new RemoveKnowledgeArticle());
        this.registerHandler(httpServer, new ActivateKnowledgeArticle());
        this.registerHandler(httpServer, new DeactivateKnowledgeArticle());
        this.registerHandler(httpServer, new QueryAllArticleCategories());
        this.registerHandler(httpServer, new GenerateKnowledge());
        this.registerHandler(httpServer, new SearchResults());
        this.registerHandler(httpServer, new ContextInference());
        this.registerHandler(httpServer, new ChartData());
        this.registerHandler(httpServer, new Prompts());
        this.registerHandler(httpServer, new SubmitEvent());
        this.registerHandler(httpServer, new QueryAppEvents());
        this.registerHandler(httpServer, new QueryUsages());
        this.registerHandler(httpServer, new ChatHistory());
        this.registerHandler(httpServer, new TextToFile());
        this.registerHandler(httpServer, new GetQueueCount());
        this.registerHandler(httpServer, new ApplyStream());

        // ⚠️ 语音基础能力的端点（语音识别 /aigc/speech/recognition、
        // 说话人分离 /aigc/speech/diarization 及其 opt、情绪识别
        // /aigc/speech/emotion、语音流申请 /aigc/stream/apply/）
        // 属宿主功能实现，仍在本类内联注册，不经插件注入。
        // 另：/aigc/chart/data 由宿主图表任务处理，亦保留在此。
        //
        // ⚠️ 与之相对，「分析语音内容」（/aigc/speech/analysis）与
        // 「停止语音流」（/aigc/stream/stop/）路由的是心理学插件的动作，
        // 注册权已移交插件；说话人视图 /aigc/chart/ 与 /aigc/cot/ 同理。
        // ⚠️ 绘画图表数据（/aigc/chart/data）与说话人视图
        //（/aigc/chart/、/aigc/cot/）中，前者由宿主图表任务处理，
        // 保留在此；后两者路由插件动作，已移交插件。

        // 业务模块兜底通道 POST /aigc/module/{moduleName}/{actionName}。
        //
        // ⚠️ 受配置开关控制（config/dispatcher.properties 的 module.rest.enabled），
        // 默认关闭：关闭时该路径不注册，专属端点不受影响。
        // 专属端点（上面 16 条）始终保留，兜底通道不替代它们。
        if (ModuleAction.isEnabled()) {
            this.registerHandler(httpServer, new ModuleAction());
            Logger.i(Manager.class, "#setupHandler - Module fallback channel enabled at " + ModuleAction.PATH);
        }
        else {
            Logger.i(Manager.class, "#setupHandler - Module fallback channel is DISABLED");
        }

        // 业务模块的网关扩展：模块声明自己拥有的 REST 前缀，并提供自己的
        // ContextHandler 实例；本侧只按类名反射装载、做路径去重后注册。
        // 详见 cube.dispatcher.aigc.spi.DispatcherExtension 的 javadoc。
        this.dispatcherExtensions = DispatcherExtensions.load();
        Set<String> modulePrefixes = DispatcherExtensions.collectPrefixes(this.dispatcherExtensions);
        if (!modulePrefixes.isEmpty()) {
            Logger.i(Manager.class, "#setupHandler - Module REST prefixes: " + modulePrefixes);
        }

        this.registerHandler(httpServer, new cube.dispatcher.aigc.handler.app.Activate());
        this.registerHandler(httpServer, new cube.dispatcher.aigc.handler.app.User());
        this.registerHandler(httpServer, new cube.dispatcher.aigc.handler.app.UserModify());
        this.registerHandler(httpServer, new cube.dispatcher.aigc.handler.app.Profile());
        this.registerHandler(httpServer, new cube.dispatcher.aigc.handler.app.Membership());
        this.registerHandler(httpServer, new cube.dispatcher.aigc.handler.app.AppVersion());
        this.registerHandler(httpServer, new cube.dispatcher.aigc.handler.app.ASCIIArt());
        this.registerHandler(httpServer, new cube.dispatcher.aigc.handler.app.WordCloud());
        this.registerHandler(httpServer, new cube.dispatcher.aigc.handler.app.Emotion());
        this.registerHandler(httpServer, new cube.dispatcher.aigc.handler.app.UserSignOut());
        this.registerHandler(httpServer, new cube.dispatcher.aigc.handler.app.Session());
        this.registerHandler(httpServer, new cube.dispatcher.aigc.handler.app.Verify());
        this.registerHandler(httpServer, new cube.dispatcher.aigc.handler.app.Config());
        this.registerHandler(httpServer, new cube.dispatcher.aigc.handler.app.Change());
        this.registerHandler(httpServer, new cube.dispatcher.aigc.handler.app.Chat());
        this.registerHandler(httpServer, new cube.dispatcher.aigc.handler.app.Evaluate());
        this.registerHandler(httpServer, new cube.dispatcher.aigc.handler.app.KeepAlive());
        this.registerHandler(httpServer, new cube.dispatcher.aigc.handler.app.Inject());

        // 业务模块自有的 REST 端点：排在全部宿主端点之后，
        // 使宿主端点在路径重复时天然优先（重复项会被记 ERROR 并跳过）。
        DispatcherExtensions.registerEndpointHandlers(this.dispatcherExtensions, httpServer);
    }

    public JSONObject syncRequest(String token, AIGCAction action, JSONObject data) {
        Packet packet = new Packet(action.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 90 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#syncRequest - Response is null, action: " + action.name);
            return null;
        }

        Packet responsePacket = new Packet(response);
        int state = Packet.extractCode(responsePacket);
        if (AIGCStateCode.Ok.code != state) {
            Logger.w(Manager.class, "#syncRequest - " + action.name + " response state is " + state);
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public boolean checkToken(String token, Device device) {
        return (null != this.checkAndGetToken(token, device));
    }

    public String checkAndGetToken(String token, Device device) {
        ContactToken contactToken = this.resolveContactToken(token, device);
        return null == contactToken ? null : contactToken.authToken.getCode();
    }

    private ContactToken resolveContactToken(String token, Device device) {
        if (isBlank(token) || null == device || isBlank(device.getName())) {
            return null;
        }
        final long requestGeneration;
        final Performer current;
        synchronized (this) {
            if (!this.running) {
                return null;
            }
            requestGeneration = this.generation;
            current = this.performer;
            ContactToken contactToken = this.validTokenMap.get(token);
            if (null != contactToken) {
                if (isTokenExpired(contactToken, System.currentTimeMillis())) {
                    this.validTokenMap.remove(token, contactToken);
                }
                else if (device.isUnknown() ||
                        device.getName().equalsIgnoreCase(contactToken.device.getName())) {
                    return contactToken;
                }
            }
        }
        try {
            JSONObject data = new JSONObject();
            data.put(token.length() == 6 ? "invitation" : "token", token);
            data.put("device", device.toJSON());
            Packet packet = new Packet(AIGCAction.CheckToken.name, data);
            ActionDialect response = current.syncTransmit(AIGCCellet.NAME, packet.toDialect());
            if (null == response) {
                return null;
            }
            Packet responsePacket = new Packet(response);
            if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
                return null;
            }
            JSONObject payload = Packet.extractDataPayload(responsePacket);
            JSONObject tokenJson = payload.getJSONObject("token");
            // AuthToken 构造函数吞掉 JSON 异常，在构造前校验必需字段。
            if (isBlank(tokenJson.getString("code"))) {
                return null;
            }
            tokenJson.getString("domain");
            tokenJson.getString("appKey");
            tokenJson.getLong("cid");
            tokenJson.getLong("issue");
            tokenJson.getLong("expiry");
            AuthToken authToken = new AuthToken(tokenJson);
            JSONObject contactJson = payload.getJSONObject("contact");
            contactJson.getLong("id");
            ContactToken contactToken = new ContactToken(authToken, new Contact(contactJson), device);
            synchronized (this) {
                if (!this.running || this.generation != requestGeneration ||
                        isTokenExpired(contactToken, System.currentTimeMillis())) {
                    return null;
                }
                this.validTokenMap.put(authToken.getCode(), contactToken);
                return contactToken;
            }
        } catch (RuntimeException e) {
            Logger.w(Manager.class, "#resolveContactToken - Invalid authentication response", e);
            return null;
        }
    }

    public ContactToken getContactToken(String token, Device device) {
        return this.resolveContactToken(token, device);
    }

    public void removeTokenCache(String token) {
        if (null != token) {
            this.validTokenMap.remove(token);
        }
    }

    private static boolean isBlank(String value) {
        return null == value || value.trim().isEmpty();
    }

    private static boolean isTokenExpired(ContactToken token, long now) {
        return now - token.timestamp >= TOKEN_CACHE_TTL || token.authToken.getExpiry() <= now;
    }

    public JSONObject getOrCreateUser(JSONObject data) {
        Packet packet = new Packet(AIGCAction.AppGetOrCreateUser.name, data);
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, packet.toDialect());
        if (null == response) {
            Logger.w(Manager.class, "#getOrCreateUser - Response is null");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#getOrCreateUser - Response state is NOT ok : "
                    + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public JSONObject modifyUser(String token, JSONObject modification) {
        Packet packet = new Packet(AIGCAction.AppModifyUser.name, modification);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(Manager.class, "#modifyUser - Response is null");
            return null;
        }

        Packet responsePacket = new Packet(response);
        int state = Packet.extractCode(responsePacket);
        if (AIGCStateCode.Ok.code != state) {
            Logger.w(Manager.class, "#modifyUser - Response state is " + state);
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public JSONObject checkInUser(String token, JSONObject data, String address) {
        data.put("address", address);
        Packet packet = new Packet(AIGCAction.AppCheckInUser.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(Manager.class, "#checkInUser - Response is null");
            return null;
        }

        Packet responsePacket = new Packet(response);
        int state = Packet.extractCode(responsePacket);
        if (AIGCStateCode.IllegalOperation.code == state) {
            JSONObject json = new JSONObject();
            json.put("valid", false);
            return json;
        }
        else if (AIGCStateCode.Ok.code == state) {
            JSONObject json = new JSONObject();
            json.put("valid", true);
            json.put("user", Packet.extractDataPayload(responsePacket));
            return json;
        }

        Logger.w(Manager.class, "#checkInUser - Response state is " + state);
        return null;
    }

    /**
     * 注销用户。
     *
     * @param token
     * @return
     */
    public JSONObject signOutUser(String token) {
        Packet packet = new Packet(AIGCAction.AppSignOutUser.name, new JSONObject());
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(Manager.class, "#signOutUser - Response is null");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#signOutUser - Response state is NOT ok : "
                    + Packet.extractCode(responsePacket));
            return null;
        }

        this.removeTokenCache(token);
        return Packet.extractDataPayload(responsePacket);
    }

    public UserProfile getUserProfile(String token) {
        Packet packet = new Packet(AIGCAction.AppGetUserProfile.name, new JSONObject());
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(Manager.class, "#getUserProfile - Response is null");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#getUserProfile - Response state is NOT ok : "
                    + Packet.extractCode(responsePacket));
            return null;
        }

        return new UserProfile(Packet.extractDataPayload(responsePacket));
    }

    public Membership activateMembership(String token, String channel, String invitation) {
        JSONObject data = new JSONObject();
        data.put("channel", channel);
        data.put("invitation", invitation);
        Packet packet = new Packet(AIGCAction.AppActivateMembership.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(Manager.class, "#activateMembership - Response is null");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#activateMembership - Response state is NOT ok : "
                    + Packet.extractCode(responsePacket));
            return null;
        }

        return new Membership(Packet.extractDataPayload(responsePacket));
    }

    public JSONObject getAppVersion(String token) {
        Packet packet = new Packet(AIGCAction.AppVersion.name, new JSONObject());
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(Manager.class, "#getAppVersion - Response is null");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#getAppVersion - Response state is NOT ok : "
                    + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public JSONObject getASCIIArt(String token) {
        Packet packet = new Packet(AIGCAction.AppASCIIArt.name, new JSONObject());
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(Manager.class, "#getASCIIArt - Response is null");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#getASCIIArt - Response state is NOT ok : "
                    + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public JSONObject getWordCloud(String token) {
        Packet packet = new Packet(AIGCAction.GetWordCloud.name, new JSONObject());
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request);
        if (null == response) {
            Logger.w(Manager.class, "#getWordCloud - Response is null");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#getWordCloud - Response state is NOT ok : "
                    + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public ContactToken checkOrInjectContactToken(String phoneNumber, String userName) {
        JSONObject data = new JSONObject();
        data.put("phone", phoneNumber);
        if (null != userName) {
            data.put("name", userName);
        }

        Packet packet = new Packet(AIGCAction.AppInjectOrGetToken.name, data);
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, packet.toDialect());
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
        return new ContactToken(authToken, contact, new Device("Unknown", "Unknown"));
    }

    public ConfigInfo getConfigInfo(String token) {
        JSONObject data = new JSONObject();
        Packet packet = new Packet(AIGCAction.GetConfig.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request);
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
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
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
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request);
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
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
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

    public KnowledgeBaseInfo newKnowledgeBase(String token, String baseName, String displayName,
                                              String category, KnowledgeScope scope) {
        JSONObject payload = new JSONObject();
        payload.put("name", baseName);
        payload.put("displayName", displayName);
        if (null != category && category.length() > 0) {
            payload.put("category", category);
        }
        else {
            payload.put("category", displayName);
        }
        if (null != scope) {
            payload.put("scope", scope.name);
        }
        Packet packet = new Packet(AIGCAction.NewKnowledgeBase.name, payload);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
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
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
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
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
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
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
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
                    response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
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

    public KnowledgeDocument importKnowledgeDoc(String token, String baseName, String fileCode, TextSplitter splitter) {
        JSONObject payload = new JSONObject();
        payload.put("base", baseName);
        payload.put("fileCode", fileCode);
        payload.put("splitter", splitter.name);
        Packet packet = new Packet(AIGCAction.ImportKnowledgeDoc.name, payload);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 3 * 60 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#importKnowledgeDoc - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#importKnowledgeDoc - Response state is NOT ok : " + Packet.extractCode(responsePacket));
            return null;
        }

        return new KnowledgeDocument(Packet.extractDataPayload(responsePacket));
    }

    public KnowledgeProgress importKnowledgeDocs(String token, String baseName, JSONArray fileCodeArray, TextSplitter splitter) {
        Logger.d(this.getClass(), "#importKnowledgeDocs - " + baseName + " - " + token);

        JSONObject payload = new JSONObject();
        payload.put("base", baseName);
        payload.put("fileCodeList", fileCodeArray);
        payload.put("splitter", splitter.name);
        Packet packet = new Packet(AIGCAction.ImportKnowledgeDoc.name, payload);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
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

    public KnowledgeDocument removeKnowledgeDoc(String token, String baseName, String fileCode) {
        JSONObject payload = new JSONObject();
        payload.put("base", baseName);
        payload.put("fileCode", fileCode);
        Packet packet = new Packet(AIGCAction.RemoveKnowledgeDoc.name, payload);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
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

        return new KnowledgeDocument(Packet.extractDataPayload(responsePacket));
    }

    public KnowledgeProgress removeKnowledgeDocs(String token, String baseName, JSONArray fileCodeArray) {
        Logger.d(this.getClass(), "#removeKnowledgeDocs - " + baseName + " - " + token);

        JSONObject payload = new JSONObject();
        payload.put("base", baseName);
        payload.put("fileCodeList", fileCodeArray);
        Packet packet = new Packet(AIGCAction.RemoveKnowledgeDoc.name, payload);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
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

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
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
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
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
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
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
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
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

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
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
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
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

    public JSONObject getKnowledgeArticles(String token, String base, long startTime, long endTime, int activated) {
        JSONObject param = new JSONObject();
        param.put("base", base);
        param.put("start", startTime);
        param.put("end", endTime);
        param.put("activated", activated);
        Packet packet = new Packet(AIGCAction.ListKnowledgeArticles.name, param);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
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
                    response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
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
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
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
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
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
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
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

    public List<KnowledgeArticle> activateKnowledgeArticle(String token, String baseName, JSONArray idList) {
        List<KnowledgeArticle> result = new ArrayList<>();

        JSONObject payload = new JSONObject();
        payload.put("base", baseName);
        payload.put("ids", idList);
        Packet packet = new Packet(AIGCAction.ActivateKnowledgeArticle.name, payload);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 2 * 60 * 1000);
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

    public List<KnowledgeArticle> deactivateKnowledgeArticle(String token, String baseName, JSONArray idList) {
        List<KnowledgeArticle> result = new ArrayList<>();

        JSONObject payload = new JSONObject();
        payload.put("base", baseName);
        payload.put("ids", idList);
        Packet packet = new Packet(AIGCAction.DeactivateKnowledgeArticle.name, payload);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
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
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
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

    public JSONObject generateKnowledge(String token, String query, String baseName, int topK) {
        JSONObject param = new JSONObject();
        param.put("query", query);
        param.put("base", baseName);
        param.put("topK", topK <= 0 ? 5 : topK);
        Packet packet = new Packet(AIGCAction.GenerateKnowledge.name, param);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#generateKnowledge - Response is null : " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.d(Manager.class, "#generateKnowledge - Response state is NOT Ok : "
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
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request);
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
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request);
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
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
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
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
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
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 30 * 1000);
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

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, packet.toDialect());
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
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request);
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
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request);
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

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, packet.toDialect());
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

    public JSONObject queryQueueCount(String token) {
        Packet packet = new Packet(AIGCAction.GetQueueCount.name, new JSONObject());
        ActionDialect dialect = packet.toDialect();
        dialect.addParam("token", token);
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, dialect);
        if (null == response) {
            Logger.w(Manager.class, "#queryQueueCount - Response is null : " + token);
            return null;
        }
        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(Manager.class, "#queryQueueCount - Response state code is NOT Ok : " + Packet.extractCode(responsePacket));
            return null;
        }
        return Packet.extractDataPayload(responsePacket);
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
     * @param networking
     * @param categories
     * @return
     */
    public ChatFuture chat(String token, String channelCode, String pattern, String content, String unit,
                           GeneratingOption option, int histories, JSONArray records,
                           boolean recordable, boolean networking, JSONArray categories) {
        JSONObject data = new JSONObject();
        data.put("token", token);
        data.put("channel", channelCode);
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
        data.put("networking", networking);

        Packet responsePacket = null;
        Packet packet = new Packet(AIGCAction.Chat.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 5 * 60 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#chat - Response is null - " + channelCode);
            return null;
        }

        responsePacket = new Packet(response);

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
                    GeneratingRecord record = new GeneratingRecord(responseData);
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
     * 多模态对话。
     *
     * @param token
     * @param channelCode
     * @param input
     * @return
     */
    public MultimodalOutput executeMultimodal(String token, String channelCode, MultimodalInput input) {
        JSONObject data = new JSONObject();
        data.put("token", token);
        data.put("channel", channelCode);
        data.put("input", input.toJSON());

        Packet packet = new Packet(AIGCAction.Multimodal.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 3 * 60 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#executeMultimodal - Response is null - " + channelCode);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(Manager.class, "#executeMultimodal - Response state code is NOT Ok - " + channelCode +
                    " - " + Packet.extractCode(responsePacket));
            return null;
        }

        JSONObject responseData = Packet.extractDataPayload(responsePacket);
        return new MultimodalOutput(responseData);
    }

    public MultimodalOutput queryMultimodal(String token, String channelCode, long sn) {
        JSONObject data = new JSONObject();
        data.put("token", token);
        data.put("code", channelCode);
        data.put("sn", sn);

        Packet packet = new Packet(AIGCAction.QueryMultimodal.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(Manager.class, "#queryMultimodal - Response is null - " + channelCode);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(Manager.class, "#queryMultimodal - Response state code is NOT Ok - " + channelCode +
                    " - " + Packet.extractCode(responsePacket));
            return null;
        }

        return new MultimodalOutput(Packet.extractDataPayload(responsePacket));
    }

    public JSONObject querySearchResults(String token) {
        Packet packet = new Packet(AIGCAction.GetSearchResults.name, new JSONObject());
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request);
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

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request);
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

    public KnowledgeQAProgress performKnowledgeQA(String token, String channelCode, String query,
                                                  String baseName, boolean sync) {
        JSONObject data = new JSONObject();
        data.put("channel", channelCode);
        data.put("query", query);
        data.put("topK", 5);
        data.put("sync", sync);
        if (null != baseName) {
            data.put("baseName", baseName);
        }
        Packet packet = new Packet(AIGCAction.PerformKnowledgeQA.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 4 * 60 * 1000);
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

        JSONObject responseData = Packet.extractDataPayload(responsePacket);
        return new KnowledgeQAProgress(responseData);
    }

    public KnowledgeQAProgress getKnowledgeQAProgress(String token, String channel, String baseName) {
        JSONObject data = new JSONObject();
        data.put("channel", channel);
        if (null != baseName) {
            data.put("base", baseName);
        }
        Packet packet = new Packet(AIGCAction.GetKnowledgeQAProgress.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request);
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

    public JSONObject semanticSearch(String token, String query) {
        JSONObject data = new JSONObject();
        data.put("query", query);
        Packet packet = new Packet(AIGCAction.SemanticSearch.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 90 * 1000);
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

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, packet.toDialect(), 90 * 1000);
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
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, packet.toDialect(), 120 * 100);
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

    public TextToFileFuture textToFile(String token, String text, JSONArray fileCodeList) {
        if (isBlank(token) || null == text || null == fileCodeList) {
            return null;
        }
        long sn = Utils.generateSerialNumber();
        JSONObject data = new JSONObject();
        data.put("sn", sn);
        data.put("text", text);
        data.put("files", fileCodeList);
        Packet packet = new Packet(sn, AIGCAction.TextToFile.name, data);
        return this.submitFuture(packet, token, this.textToFileFutureMap, sn,
                new TextToFileFuture(sn, token, text, fileCodeList), false);
    }

    public TextToFileFuture getTextToFileFuture(long sn) {
        return this.textToFileFutureMap.get(sn);
    }

    public SpeechRecognitionFuture automaticSpeechRecognition(String token, String fileCode, String fileUrl,
                                                              boolean sync, boolean reset) {
        if (isBlank(token) || isBlank(null != fileCode ? fileCode : fileUrl)) {
            return null;
        }
        JSONObject data = new JSONObject();
        data.put(null != fileCode ? "fileCode" : "fileUrl", null != fileCode ? fileCode : fileUrl);
        Packet packet = new Packet(AIGCAction.AutomaticSpeechRecognition.name, data);
        if (sync) {
            ActionDialect request = packet.toDialect();
            request.addParam("token", token);
            ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
            if (null == response) {
                return null;
            }
            try {
                Packet responsePacket = new Packet(response);
                if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
                    return null;
                }
                SpeechRecognitionInfo result = new SpeechRecognitionInfo(Packet.extractDataPayload(responsePacket));
                if (null == result.file) {
                    return null;
                }
                return new SpeechRecognitionFuture(token, fileCode, fileUrl, result);
            } catch (RuntimeException e) {
                Logger.w(Manager.class, "#automaticSpeechRecognition - Invalid response", e);
                return null;
            }
        }
        String queryCode = FileUtils.fastHash(null != fileCode ? fileCode : fileUrl);
        return this.submitFuture(packet, token, this.speechRecognitionFutureMap, queryCode,
                new SpeechRecognitionFuture(token, fileCode, fileUrl, queryCode), reset);
    }

    public SpeechRecognitionFuture getSpeechRecognitionFuture(String queryCode) {
        return null == queryCode ? null : this.speechRecognitionFutureMap.get(queryCode);
    }

    public SpeechEmotionRecognitionFuture speechEmotionRecognition(String token, String fileCode, boolean reset) {
        if (isBlank(token) || isBlank(fileCode)) {
            return null;
        }
        JSONObject payload = new JSONObject();
        payload.put("fileCode", fileCode);
        return this.submitFuture(new Packet(AIGCAction.SpeechEmotionRecognition.name, payload), token,
                this.speechEmotionRecognitionFutureMap, fileCode,
                new SpeechEmotionRecognitionFuture(token, fileCode), reset);
    }

    public SpeechEmotionRecognitionFuture getSpeechEmotionRecognitionFuture(String fileCode) {
        return null == fileCode ? null : this.speechEmotionRecognitionFutureMap.get(fileCode);
    }

    public FacialExpressionRecognitionFuture facialExpressionRecognition(String token, String fileCode,
                                                                         boolean visualize, boolean reset) {
        if (isBlank(token) || isBlank(fileCode)) {
            return null;
        }
        JSONObject payload = new JSONObject();
        payload.put("fileCode", fileCode);
        payload.put("visualize", visualize);
        return this.submitFuture(new Packet(AIGCAction.FacialExpressionRecognition.name, payload), token,
                this.facialExpressionRecognitionFutureMap, fileCode,
                new FacialExpressionRecognitionFuture(token, fileCode), reset);
    }

    public FacialExpressionRecognitionFuture getFacialExpressionRecognitionFuture(String fileCode) {
        return null == fileCode ? null : this.facialExpressionRecognitionFutureMap.get(fileCode);
    }

    public SpeechDiarizationFuture speechDiarization(String token, String fileCode, String fileUrl, boolean reset) {
        if (isBlank(token) || isBlank(null != fileCode ? fileCode : fileUrl)) {
            return null;
        }
        String queryCode = FileUtils.fastHash(null != fileCode ? fileCode : fileUrl);
        JSONObject payload = new JSONObject();
        payload.put(null != fileCode ? "fileCode" : "fileUrl", null != fileCode ? fileCode : fileUrl);
        return this.submitFuture(new Packet(AIGCAction.SpeechDiarization.name, payload), token,
                this.speechDiarizationFutureMap, queryCode,
                new SpeechDiarizationFuture(token, fileCode, fileUrl, queryCode), reset);
    }

    public SpeechDiarizationFuture getSpeechDiarization(String token, String queryCode) {
        if (isBlank(token) || isBlank(queryCode)) {
            return null;
        }
        SpeechDiarizationFuture future = null;
        if (queryCode.length() == 64) {
            future = this.speechDiarizationFutureMap.get(FileUtils.fastHash(queryCode));
        }
        else {
            future = this.speechDiarizationFutureMap.get(queryCode);
        }
        if (null != future) {
            return future;
        }

        JSONObject payload = new JSONObject();
        payload.put("fileCode", queryCode);
        Packet packet = new Packet(AIGCAction.GetSpeechDiarization.name, payload);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#getSpeechDiarization - No response: " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#getSpeechDiarization - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        JSONObject responseData = Packet.extractDataPayload(responsePacket);
        if (responseData.has("file")) {
            FileLabels.reviseFileLabel(responseData.getJSONObject("file"), token,
                    getPerformer().getExternalHttpEndpoint(),
                    getPerformer().getExternalHttpsEndpoint());
        }
        VoiceDiarization diarization = new VoiceDiarization(responseData);
        future = new SpeechDiarizationFuture(diarization.getTimestamp(),
                token, diarization.fileCode, AIGCStateCode.Ok, diarization);
        return future;
    }

    public JSONObject listSpeechDiarizations(String token) {
        JSONObject payload = new JSONObject();
        Packet packet = new Packet(AIGCAction.ListSpeechDiarizations.name, payload);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 2 * 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#listSpeechDiarizations - No response: " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#listSpeechDiarizations - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        JSONObject responseData = Packet.extractDataPayload(responsePacket);
        JSONArray list = responseData.getJSONArray("list");
        for (int i = 0; i < list.length(); ++i) {
            JSONObject json = list.getJSONObject(i);
            if (json.has("file")) {
                FileLabels.reviseFileLabel(json.getJSONObject("file"), token,
                        getPerformer().getExternalHttpEndpoint(),
                        getPerformer().getExternalHttpsEndpoint());
            }
        }
        return responseData;
    }

    public JSONObject deleteSpeechDiarization(String token, String fileCode) {
        JSONObject payload = new JSONObject();
        payload.put("fileCode", fileCode);
        Packet packet = new Packet(AIGCAction.DeleteSpeechDiarization.name, payload);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 2 * 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#deleteSpeechDiarization - No response: " + token);
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#deleteSpeechDiarization - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        JSONObject responseData = Packet.extractDataPayload(responsePacket);
        if (responseData.has("file")) {
            FileLabels.reviseFileLabel(responseData.getJSONObject("file"), token,
                    getPerformer().getExternalHttpEndpoint(),
                    getPerformer().getExternalHttpsEndpoint());
        }
        VoiceDiarization diarization = new VoiceDiarization(responseData);
        SpeechDiarizationFuture future = new SpeechDiarizationFuture(diarization.getTimestamp(),
                token, diarization.fileCode, AIGCStateCode.Ok, diarization);
        return future.toJSON();
    }

    public JSONObject getUserEmotionData(String token) {
        JSONObject result = new JSONObject();
        JSONArray data = new JSONArray();

        Packet packet = new Packet(AIGCAction.GetEmotionRecords.name, new JSONObject());
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);
        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 3 * 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#getUserEmotionData - No response: " + token);
            return null;
        }
        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#getUserEmotionData - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }
        JSONObject emotionRecords = Packet.extractDataPayload(responsePacket);
        JSONArray array = emotionRecords.getJSONArray("list");
        List<EmotionRecord> emotionRecordList = new ArrayList<>();
        for (int i = 0; i < array.length(); ++i) {
            emotionRecordList.add(new EmotionRecord(array.getJSONObject(i)));
        }

        for (Emotion emotion : Emotion.values()) {
            if (emotion == Emotion.Unknown || emotion == Emotion.None) {
                continue;
            }
            String name = emotion.name();
            int value = 0;
            for (EmotionRecord er : emotionRecordList) {
                if (er.emotion == emotion) {
                    value += 1;
                }
            }
            JSONObject emotionJson = new JSONObject();
            emotionJson.put("name", name);
            emotionJson.put("value", value);
            data.put(emotionJson);
        }

        result.put("data", data);
        result.put("timestamp", System.currentTimeMillis());
        result.put("num", emotionRecordList.size());
        return result;
    }

    public JSONObject segmentation(String token, String text) {
        JSONObject data = new JSONObject();
        data.put("text", text);
        Packet packet = new Packet(AIGCAction.Segmentation.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request);
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

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request);
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

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request);
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

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request);
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

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request);
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
        data.put("title", promptRecord.title);
        data.put("content", promptRecord.content);
        data.put("act", promptRecord.act);

        Packet packet = new Packet(AIGCAction.SetPrompts.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request);
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

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request);
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

    /*
     * @deprecated
     * @param token
     * @param moduleName
     * @param param
     * @return
     */
    /*public JSONObject inferByModule(String token, String moduleName, JSONObject param) {
        JSONObject data = new JSONObject();
        data.put("module", moduleName);
        data.put("param", param);
        Packet packet = new Packet(AIGCAction.InferByModule.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request);
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
    }*/

    /**
     * 生成心理测验报告。
     *
     * @param remote
     * @param token
     * @param attribute
     * @param fileCode
     * @param theme
     * @param indicators
     * @param adjust
     * @param remark
     * @return
     */
    public PaintingReport generatePsychologyReport(String remote, String token, Attribute attribute,
                                                   String fileCode, String theme, int indicators,
                                                   boolean adjust, String remark) {
        JSONObject data = new JSONObject();
        data.put("remote", remote);
        data.put("attribute", attribute.toJSON());
        data.put("fileCode", fileCode);
        data.put("theme", theme);
        data.put("indicators", indicators);
        data.put("adjust", adjust);
        if (null != remark) {
            data.put("remark", remark);
        }

        Packet packet = new Packet(AIGCAction.GeneratePsychologyReport.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
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

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
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
     * @param type
     * @param pageIndex
     * @param pageSize
     * @param descending
     * @return
     */
    public JSONObject getPsychologyReports(String token, String type, int pageIndex, int pageSize, boolean descending) {
        if (pageSize <= 0) {
            Logger.w(this.getClass(), "#getPsychologyReports - page size is zero: " + token);
            return null;
        }

        JSONObject data = new JSONObject();
        data.put("type", type);
        data.put("page", pageIndex);
        data.put("size", pageSize);
        data.put("desc", descending);
        data.put("state", 0);
        Packet packet = new Packet(AIGCAction.GetPsychologyReport.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#getPsychologyReports - No response: " + token);
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

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
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
        data.put("sections", false);
        data.put("markdown", false);
        Packet packet = new Packet(AIGCAction.GetPsychologyReport.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
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

        // 第二步，获取报告数据各段落
        data = new JSONObject();
        data.put("sn", sn);
        data.put("sections", true);
        data.put("markdown", false);
        packet = new Packet(AIGCAction.GetPsychologyReport.name, data);
        request = packet.toDialect();
        request.addParam("token", token);

        response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#getPsychologyReport - No response");
            return null;
        }

        responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#getPsychologyReport - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        report.extendReportSections(Packet.extractDataPayload(responsePacket));

        // 第三步，是否获取 Markdown 数据
        if (markdown) {
            data = new JSONObject();
            data.put("sn", sn);
            data.put("sections", false);
            data.put("markdown", true);
            packet = new Packet(AIGCAction.GetPsychologyReport.name, data);
            request = packet.toDialect();
            request.addParam("token", token);

            response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
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

    public JSONObject getPsychologyReportPart(String token, long sn, boolean content, boolean section, boolean thought,
                                              boolean summary, boolean rating,  boolean link) {
        Performer current = this.activePerformer();
        if (null == current) {
            return null;
        }

        JSONObject data = new JSONObject();
        data.put("sn", sn);
        data.put("content", content);
        data.put("section", section);
        data.put("thought", thought);
        data.put("summary", summary);
        data.put("rating", rating);
        data.put("link", link);
        data.put("endpoint", current.getExternalHttpsEndpoint().toJSON());
        Packet packet = new Packet(AIGCAction.GetPsychologyReportPart.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
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

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
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

    public JSONObject resetReportAttention(String token, long sn, Attention attention) {
        JSONObject data = new JSONObject();
        data.put("sn", sn);
        if (null != attention) {
            data.put("attention", attention.level);
        }
        Packet packet = new Packet(AIGCAction.ResetReportAttention.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#resetReportAttention - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#resetReportAttention - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        return Packet.extractDataPayload(responsePacket);
    }

    public JSONObject modifyReportRemark(String token, JSONObject data) {
        Packet packet = new Packet(AIGCAction.ModifyReportRemark.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#modifyReportRemark - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#modifyReportRemark - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }
        return Packet.extractDataPayload(responsePacket);
    }

    public JSONObject listPsychologyScales(String token) {
        Packet packet = new Packet(AIGCAction.ListPsychologyScales.name, new JSONObject());
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
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

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
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

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
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

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
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
                                                    JSONArray relations, String query) {
        Performer current = this.activePerformer();
        if (null == current) {
            return null;
        }

        JSONObject endpoint = new JSONObject();
        endpoint.put("http", current.getExternalHttpEndpoint().toJSON());
        endpoint.put("https", current.getExternalHttpsEndpoint().toJSON());

        JSONObject data = new JSONObject();
        data.put("channelCode", channelCode);
        data.put("endpoint", endpoint);
        data.put("relations", relations);
        data.put("query", query);
        Packet packet = new Packet(AIGCAction.PsychologyConversation.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 3 * 60 * 1000);
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

    public JSONObject executePsychologyConversation(String token, String channelCode,
                                                    JSONObject context, JSONObject relation, String query) {
        Performer current = this.activePerformer();
        if (null == current) {
            return null;
        }

        if (null == relation) {
            Logger.w(this.getClass(), "#executePsychologyConversation - The relation is null");
            return null;
        }

        JSONObject endpoint = new JSONObject();
        endpoint.put("http", current.getExternalHttpEndpoint().toJSON());
        endpoint.put("https", current.getExternalHttpsEndpoint().toJSON());

        JSONObject data = new JSONObject();
        data.put("channelCode", channelCode);
        data.put("endpoint", endpoint);
        if (null != context) {
            data.put("context", context);
        }
        data.put("relation", relation);
        data.put("query", query);
        Packet packet = new Packet(AIGCAction.PsychologyConversation.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 3 * 60 * 1000);
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

    public JSONObject getPsychologyPainting(String token, String fileCode) {
        JSONObject data = new JSONObject();
        data.put("fileCode", fileCode);
        Packet packet = new Packet(AIGCAction.GetPsychologyPainting.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 120 * 1000);
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

    public JSONObject getPsychologyPainting(String token, long reportSn, boolean bbox, boolean vparam, double prob) {
        JSONObject data = new JSONObject();
        data.put("sn", reportSn);
        data.put("bbox", bbox);
        data.put("vparam", vparam);
        data.put("prob", prob);
        Packet packet = new Packet(AIGCAction.GetPsychologyPainting.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
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

    public JSONObject getPsychologyPaintingChart(String token, long reportSn) {
        JSONObject data = new JSONObject();
        data.put("sn", reportSn);
        data.put("chart", true);
        Packet packet = new Packet(AIGCAction.GetPsychologyPainting.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#getPsychologyPaintingChart - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#getPsychologyPaintingChart - Response state is " + Packet.extractCode(responsePacket));
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

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 90 * 1000);
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

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 90 * 1000);
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

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 60 * 1000);
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

    public boolean applyStream(String token, Device device, String streamType, String streamName) {
        Performer current;
        long requestGeneration;
        synchronized (this) {
            if (!this.running || isBlank(streamType) || isBlank(streamName)) {
                return false;
            }
            current = this.performer;
            requestGeneration = this.generation;
        }
        if (null == StreamType.parse(streamType)) {
            return false;
        }
        ContactToken contactToken = this.getContactToken(token, device);
        if (null == contactToken) {
            return false;
        }
        synchronized (this) {
            if (!this.running || this.generation != requestGeneration) {
                return false;
            }
            Cellet cellet = current.getCellet(AIGCCellet.NAME);
            if (!(cellet instanceof AIGCCellet) || null == ((AIGCCellet) cellet).getStreamProcessor()) {
                return false;
            }
            ((AIGCCellet) cellet).getStreamProcessor().register(streamName, contactToken.authToken);
            return true;
        }
    }

    public boolean stopStream(String token, String streamName) {
        JSONObject data = new JSONObject();
        data.put("streamName", streamName);
        Packet packet = new Packet(AIGCAction.StopVoiceStream.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 3 * 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#stopStream - No response");
            return false;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#stopStream - Response state is " + Packet.extractCode(responsePacket));
            return false;
        }

        return true;
    }

    public FileLabel getStreamFile(String token, String streamName) {
        JSONObject data = new JSONObject();
        data.put("streamName", streamName);
        Packet packet = new Packet(AIGCAction.GetVoiceStreamFile.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 3 * 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#getStreamFile - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#getStreamFile - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        JSONObject json = Packet.extractDataPayload(responsePacket);
        return new FileLabel(json);
    }

    /**
     * 查询咨询策略。
     *
     * @param token
     * @param streamName
     * @param theme
     * @param attribute
     * @param index
     * @return
     */
    public CounselingStrategy queryCounselingStrategy(String token, String streamName, ConsultationTheme theme,
                                                      Attribute attribute, int index) {
        JSONObject data = new JSONObject();
        data.put("streamName", streamName);
        data.put("theme", theme.code);
        data.put("attribute", attribute.toJSON());
        data.put("index", index);
        Packet packet = new Packet(AIGCAction.QueryCounselingStrategy.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 3 * 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#queryCounselingStrategy - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#queryCounselingStrategy - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        return new CounselingStrategy(Packet.extractDataPayload(responsePacket));
    }

    /**
     * 查询咨询提示性说明。
     *
     * @param token
     * @param streamName
     * @param theme
     * @param attribute
     * @param consultingAction
     * @param index
     * @return
     */
    public CounselingStrategy queryCounselingCaption(String token, String streamName, ConsultationTheme theme,
                                                     Attribute attribute,
                                                     CounselingStrategy.ConsultingAction consultingAction,
                                                     int index) {
        JSONObject data = new JSONObject();
        data.put("streamName", streamName);
        data.put("theme", theme.code);
        data.put("attribute", attribute.toJSON());
        data.put("consultingAction", consultingAction.code);
        data.put("index", index);
        Packet packet = new Packet(AIGCAction.QueryCounselingCaption.name, data);
        ActionDialect request = packet.toDialect();
        request.addParam("token", token);

        ActionDialect response = this.syncTransmit(AIGCCellet.NAME, request, 3 * 60 * 1000);
        if (null == response) {
            Logger.w(this.getClass(), "#queryCounselingCaption - No response");
            return null;
        }

        Packet responsePacket = new Packet(response);
        if (Packet.extractCode(responsePacket) != AIGCStateCode.Ok.code) {
            Logger.w(this.getClass(), "#queryCounselingCaption - Response state is " + Packet.extractCode(responsePacket));
            return null;
        }

        return new CounselingStrategy(Packet.extractDataPayload(responsePacket));
    }

    @Override
    public synchronized void onTick(long now) {
        if (!this.running) {
            return;
        }
        if (now - this.lastTickTime >= 60 * 1000) {
            this.lastTickTime = now;
            for (Map.Entry<String, ContactToken> entry : this.validTokenMap.entrySet()) {
                if (isTokenExpired(entry.getValue(), now)) {
                    this.validTokenMap.remove(entry.getKey(), entry.getValue());
                }
            }
            this.clearExpiredFutures(this.textToFileFutureMap, now);
            this.clearExpiredFutures(this.speechRecognitionFutureMap, now);
            this.clearExpiredFutures(this.speechEmotionRecognitionFutureMap, now);
            this.clearExpiredFutures(this.speechDiarizationFutureMap, now);
            this.clearExpiredFutures(this.facialExpressionRecognitionFutureMap, now);
        }
        App.getInstance().onTick(now);
    }

    private <K, F extends JSONable> void clearExpiredFutures(Map<K, F> cache, long now) {
        for (Map.Entry<K, F> entry : cache.entrySet()) {
            F future = entry.getValue();
            if (now - futureTimestamp(future) >= FUTURE_CACHE_TTL && cache.remove(entry.getKey(), future)) {
                this.detachFuture(future, AIGCStateCode.Expired);
            }
        }
    }

    @Override
    public synchronized void onReceived(String cellet, Primitive primitive) {
        if (!this.running || !AIGCCellet.NAME.equals(cellet) || null == primitive) {
            return;
        }
        PendingRequest pending = null;
        try {
            ActionDialect response = new ActionDialect(primitive);
            long sn = response.getParamAsLong("sn");
            pending = this.pendingRequests.get(sn);
            if (null == pending || !pending.action.equals(response.getName()) || !pending.isCurrent()) {
                return;
            }
            if (System.currentTimeMillis() - futureTimestamp(pending.future) >= FUTURE_CACHE_TTL) {
                pending.cache.remove(pending.key, pending.future);
                pending.fail(AIGCStateCode.Expired);
                this.removePending(pending);
                return;
            }
            Packet packet = new Packet(response);
            int code = Packet.extractCode(packet);
            if (code == AIGCStateCode.Processing.code || code == AIGCStateCode.Inferencing.code) {
                return;
            }
            if (code == AIGCStateCode.Ok.code) {
                this.completeFuture(pending.future, Packet.extractDataPayload(packet));
            }
            else {
                if (pending.future instanceof TextToFileFuture) {
                    synchronized (pending.future) {
                        ((TextToFileFuture) pending.future).stateCode = code;
                    }
                }
                else {
                    pending.fail(AIGCStateCode.parse(code));
                }
            }
            this.removePending(pending);
        } catch (RuntimeException e) {
            if (null != pending && pending.isCurrent()) {
                pending.fail(AIGCStateCode.DataStructureError);
                this.removePending(pending);
                Logger.w(Manager.class, "#onReceived - Invalid response, action: " + pending.action +
                        ", sn: " + pending.sn, e);
            }
        }
    }

    /** 在同一对象锁中发布结果和完成状态，与 toJSON 保持一致。 */
    private void completeFuture(JSONable future, JSONObject data) {
        synchronized (future) {
            if (future instanceof TextToFileFuture) {
                TextToFileFuture f = (TextToFileFuture) future;
                JSONObject result = data.getJSONObject("result");
                for (String name : Arrays.asList("fileLabels", "answerFileLabels", "queryFileLabels")) {
                    if (result.has(name)) {
                        JSONArray files = result.getJSONArray(name);
                        for (int i = 0; i < files.length(); ++i) {
                            FileLabels.reviseFileLabel(files.getJSONObject(i), f.token,
                                    this.performer.getExternalHttpEndpoint(), this.performer.getExternalHttpsEndpoint());
                        }
                    }
                }
                f.result = result;
                f.stateCode = AIGCStateCode.Ok.code;
            }
            else if (future instanceof SpeechRecognitionFuture) {
                SpeechRecognitionInfo result = new SpeechRecognitionInfo(data);
                if (null == result.file) {
                    throw new IllegalArgumentException("Missing speech recognition file");
                }
                SpeechRecognitionFuture f = (SpeechRecognitionFuture) future;
                f.result = result;
                f.stateCode = AIGCStateCode.Ok;
            }
            else if (future instanceof SpeechEmotionRecognitionFuture) {
                SpeechEmotion result = new SpeechEmotion(data);
                if (null == result.file) {
                    throw new IllegalArgumentException("Missing speech emotion file");
                }
                SpeechEmotionRecognitionFuture f = (SpeechEmotionRecognitionFuture) future;
                f.result = result;
                f.stateCode = AIGCStateCode.Ok;
            }
            else if (future instanceof SpeechDiarizationFuture) {
                VoiceDiarization result = new VoiceDiarization(data);
                if (null == result.file) {
                    throw new IllegalArgumentException("Missing diarization file");
                }
                SpeechDiarizationFuture f = (SpeechDiarizationFuture) future;
                f.diarization = result;
                f.stateCode = AIGCStateCode.Ok;
            }
            else if (future instanceof FacialExpressionRecognitionFuture) {
                FacialExpressionResult result = new FacialExpressionResult(data);
                FacialExpressionRecognitionFuture f = (FacialExpressionRecognitionFuture) future;
                f.result = result;
                f.stateCode = AIGCStateCode.Ok;
            }
        }
    }

    public class ChatFuture implements JSONable {

        public boolean end = true;

        public AIGCChannel channel;

        public GeneratingRecord record;

        public KnowledgeQAResult knowledgeResult;

        protected ChatFuture(AIGCChannel channel) {
            this.channel = channel;
        }

        protected ChatFuture(GeneratingRecord record) {
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


    public class TextToFileFuture implements JSONable {

        protected final long sn;

        protected final long timestamp;

        protected final String token;

        protected String text;

        protected JSONArray files;

        protected JSONObject result;

        protected int stateCode = AIGCStateCode.Processing.code;

        public TextToFileFuture(long sn, String token, String text, JSONArray files) {
            this.sn = sn;
            this.token = token;
            this.timestamp = System.currentTimeMillis();
            this.text = text;
            this.files = files;
        }

        @Override
        public synchronized JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("sn", this.sn);
            json.put("timestamp", this.timestamp);
            json.put("state", this.stateCode);
            if (null != this.result) {
                json.put("result", this.result);
            }
            return json;
        }

        @Override
        public JSONObject toCompactJSON() {
            return this.toJSON();
        }
    }


    public class ObjectDetectionFuture implements JSONable {

        protected final long sn;

        protected final long timestamp;

        protected String channelCode;

        protected JSONArray fileCodeList;

        protected List<SpeechEmotion> resultList;

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
                for (SpeechEmotion result : this.resultList) {
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


    public class SpeechRecognitionFuture implements JSONable {

        protected final long timestamp;

        protected String token;

        protected String fileCode;

        protected String fileUrl;

        public String queryCode;

        protected SpeechRecognitionInfo result;

        protected AIGCStateCode stateCode = AIGCStateCode.Processing;

        protected SpeechRecognitionFuture(String token, String fileCode, String fileUrl, String queryCode) {
            this.timestamp = System.currentTimeMillis();
            this.token = token;
            this.fileCode = fileCode;
            this.fileUrl = fileUrl;
            this.queryCode = queryCode;
        }

        protected SpeechRecognitionFuture(String token, String fileCode, String fileUrl, SpeechRecognitionInfo result) {
            this.timestamp = System.currentTimeMillis();
            this.token = token;
            this.fileCode = fileCode;
            this.fileUrl = fileUrl;
            this.result = result;
            this.stateCode = AIGCStateCode.Ok;
        }

        @Override
        public synchronized JSONObject toJSON() {
            JSONObject json = new JSONObject();
            if (null != this.queryCode) {
                json.put("queryCode", this.queryCode);
            }
            json.put("timestamp", this.timestamp);
            json.put("stateCode", this.stateCode.code);
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


    public class SpeechEmotionRecognitionFuture implements JSONable {

        protected final long timestamp;

        protected String token;

        protected String fileCode;

        protected SpeechEmotion result;

        protected AIGCStateCode stateCode = AIGCStateCode.Processing;

        public SpeechEmotionRecognitionFuture(String token, String fileCode) {
            this.timestamp = System.currentTimeMillis();
            this.token = token;
            this.fileCode = fileCode;
        }

        @Override
        public synchronized JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("fileCode", this.fileCode);
            json.put("timestamp", this.timestamp);
            json.put("stateCode", this.stateCode.code);
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


    public class SpeechDiarizationFuture implements JSONable {

        protected final long timestamp;

        protected String token;

        protected String fileCode;

        protected String fileUrl;

        public String queryCode;

        protected VoiceDiarization diarization;

        protected AIGCStateCode stateCode = AIGCStateCode.Processing;

        public SpeechDiarizationFuture(String token, String fileCode, String fileUrl, String queryCode) {
            this.timestamp = System.currentTimeMillis();
            this.token = token;
            this.fileCode = fileCode;
            this.fileUrl = fileUrl;
            this.queryCode = queryCode;
        }

        public SpeechDiarizationFuture(long timestamp, String token, String fileCode, AIGCStateCode stateCode,
                                       VoiceDiarization diarization) {
            this.timestamp = timestamp;
            this.token = token;
            this.fileCode = fileCode;
            this.queryCode = FileUtils.fastHash(fileCode);
            this.stateCode = stateCode;
            this.diarization = diarization;
        }

        @Override
        public synchronized JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("queryCode", this.queryCode);
            json.put("timestamp", this.timestamp);
            json.put("stateCode", this.stateCode.code);
            if (null != this.diarization) {
                json.put("result", this.diarization.toJSON());
            }
            return json;
        }

        @Override
        public JSONObject toCompactJSON() {
            return this.toJSON();
        }
    }


    public class FacialExpressionRecognitionFuture implements JSONable {

        protected final long timestamp;

        protected String token;

        protected String fileCode;

        protected FacialExpressionResult result;

        protected AIGCStateCode stateCode = AIGCStateCode.Processing;

        public FacialExpressionRecognitionFuture(String token, String fileCode) {
            this.timestamp = System.currentTimeMillis();
            this.token = token;
            this.fileCode = fileCode;
        }

        @Override
        public synchronized JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("fileCode", this.fileCode);
            json.put("timestamp", this.timestamp);
            json.put("stateCode", this.stateCode.code);
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

        public final Device device;

        public final long timestamp = System.currentTimeMillis();

        protected ContactToken(AuthToken authToken, Contact contact, Device device) {
            this.authToken = authToken;
            this.contact = contact;
            this.device = device;
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("token", this.authToken.toJSON());
            json.put("contact", this.contact.toJSON());
            json.put("device", this.device.toJSON());
            return json;
        }
    }
}
