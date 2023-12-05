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

package cube.service.aigc;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.Notification;
import cube.aigc.PublicOpinionTaskName;
import cube.aigc.Sentiment;
import cube.aigc.attachment.ui.Event;
import cube.aigc.attachment.ui.EventResult;
import cube.aigc.psychology.Painting;
import cube.aigc.psychology.PsychologyReport;
import cube.auth.AuthToken;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.action.FileProcessorAction;
import cube.common.action.FileStorageAction;
import cube.common.entity.*;
import cube.common.notice.GetFile;
import cube.common.state.AIGCStateCode;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.file.FileProcessResult;
import cube.file.operation.AudioCropOperation;
import cube.file.operation.ExtractAudioOperation;
import cube.plugin.PluginSystem;
import cube.service.aigc.command.Command;
import cube.service.aigc.command.CommandListener;
import cube.service.aigc.knowledge.KnowledgeBase;
import cube.service.aigc.listener.*;
import cube.service.aigc.module.ModuleManager;
import cube.service.aigc.module.PublicOpinion;
import cube.service.aigc.module.Stage;
import cube.service.aigc.module.StageListener;
import cube.service.aigc.plugin.InjectTokenPlugin;
import cube.service.aigc.resource.Agent;
import cube.service.aigc.resource.ResourceAnswer;
import cube.service.aigc.scene.Evaluation;
import cube.service.aigc.scene.EvaluationReport;
import cube.service.aigc.scene.PsychologyScene;
import cube.service.aigc.scene.SceneListener;
import cube.service.auth.AuthService;
import cube.service.auth.AuthServiceHook;
import cube.service.contact.ContactManager;
import cube.service.tokenizer.Tokenizer;
import cube.storage.StorageType;
import cube.util.ConfigUtils;
import cube.util.FileType;
import cube.util.FileUtils;
import cube.util.TextUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AIGC 服务。
 */
public class AIGCService extends AbstractModule {

    public final static String NAME = "AIGC";

    private AIGCCellet cellet;

    /**
     * Key 是 AIGC 的 Query Key
     */
    private Map<String, AIGCUnit> unitMap;

    /**
     * Key 是 AIGC 的 Query Key
     */
    private Map<String, Queue<ChatUnitMeta>> chatQueueMap;

    /**
     * Key 是 AIGC 的 Query Key
     */
    private Map<String, Queue<ConversationUnitMeta>> conversationQueueMap;

    /**
     * Key 是 AIGC 的 Query Key
     */
    private Map<String, Queue<NaturalLanguageTaskMeta>> nlTaskQueueMap;

    /**
     * Key 是 AIGC 的 Query Key
     */
    private Map<String, Queue<SentimentUnitMeta>> sentimentQueueMap;

    /**
     * Key 是 AIGC 的 Query Key
     */
    private Map<String, Queue<SummarizationUnitMeta>> summarizationQueueMap;

    /**
     * Key 是 AIGC 的 Query Key
     */
    private Map<String, Queue<TextToImageUnitMeta>> textToImageQueueMap;

    /**
     * Key 是 AIGC 的 Query Key
     */
    private Map<String, Queue<ExtractKeywordsUnitMeta>> extractKeywordsQueueMap;

    /**
     * Key 是 AIGC 的 Query Key
     */
    private Map<String, Queue<ASRUnitMeta>> asrQueueMap;

    /**
     * 最大频道数量。
     */
    private int maxChannel = 2000;

    /**
     * 聊天内容最大长度限制。
     */
    private int maxChatContent = 4 * 1024;

    private ConcurrentHashMap<String, AIGCChannel> channelMap;

    private ConcurrentHashMap<String, KnowledgeBase> knowledgeMap;

    private long channelTimeout = 30 * 60 * 1000;

    private ExecutorService executor;

    private AIGCStorage storage;

    private Tokenizer tokenizer;

    /**
     * 是否访问，仅用于本地测试
     */
    private boolean useAgent = false;

    public AIGCService(AIGCCellet cellet) {
        this.cellet = cellet;
        this.unitMap = new ConcurrentHashMap<>();
        this.channelMap = new ConcurrentHashMap<>();
        this.chatQueueMap = new ConcurrentHashMap<>();
        this.conversationQueueMap = new ConcurrentHashMap<>();
        this.nlTaskQueueMap = new ConcurrentHashMap<>();
        this.sentimentQueueMap = new ConcurrentHashMap<>();
        this.summarizationQueueMap = new ConcurrentHashMap<>();
        this.textToImageQueueMap = new ConcurrentHashMap<>();
        this.extractKeywordsQueueMap = new ConcurrentHashMap<>();
        this.asrQueueMap = new ConcurrentHashMap<>();
        this.knowledgeMap = new ConcurrentHashMap<>();
        this.tokenizer = new Tokenizer();
    }

    @Override
    public void start() {
        this.executor = Executors.newCachedThreadPool();
        PsychologyScene.getInstance().setAigcService(this);

        (new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject config = ConfigUtils.readStorageConfig();
                if (config.has(AIGCService.NAME)) {
                    config = config.getJSONObject(AIGCService.NAME);
                    if (config.getString("type").equalsIgnoreCase("SQLite")) {
                        storage = new AIGCStorage(StorageType.SQLite, config);
                    }
                    else {
                        storage = new AIGCStorage(StorageType.MySQL, config);
                    }

                    storage.open();
                    storage.execSelfChecking(null);
                }
                else {
                    Logger.e(AIGCService.class, "Can NOT find AIGC storage config");
                }

                // 监听事件
                AuthService authService = (AuthService) getKernel().getModule(AuthService.NAME);
                while (!authService.isStarted()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                authService.getPluginSystem().register(AuthServiceHook.InjectToken,
                        new InjectTokenPlugin(AIGCService.this));

                // 资源管理器
                Explorer.getInstance().setup(AIGCService.this, tokenizer);

                started.set(true);
                Logger.i(AIGCService.class, "AIGC service is ready");
            }
        })).start();
    }

    @Override
    public void stop() {
        if (null != this.executor) {
            this.executor.shutdown();
            this.executor = null;
        }

        if (null != this.storage) {
            this.storage.close();
            this.storage = null;
        }

        this.started.set(false);

        Explorer.getInstance().teardown();
    }

    @Override
    public <T extends PluginSystem> T getPluginSystem() {
        return null;
    }

    @Override
    public void onTick(Module module, Kernel kernel) {
        // 周期 60 秒
//        Logger.i(AIGCService.class, "#onTick");

        long now = System.currentTimeMillis();

        // 删除失效的 Unit
        Iterator<AIGCUnit> unitIter = this.unitMap.values().iterator();
        while (unitIter.hasNext()) {
            AIGCUnit unit = unitIter.next();
            if (!unit.getContext().isValid()) {
                // 已失效
                unitIter.remove();
            }
        }

        Iterator<AIGCChannel> iter = this.channelMap.values().iterator();
        while (iter.hasNext()) {
            AIGCChannel channel = iter.next();
            if (now - channel.getActiveTimestamp() >= this.channelTimeout) {
                iter.remove();
            }
        }

        Explorer.getInstance().onTick(now);
    }

    public AIGCCellet getCellet() {
        return this.cellet;
    }

    public AIGCStorage getStorage() {
        return this.storage;
    }

    public List<AIGCUnit> setupUnit(Contact contact, List<AICapability> capabilities, TalkContext context) {
        List<AIGCUnit> result = new ArrayList<>(capabilities.size());

        for (AICapability capability : capabilities) {
            String key = AIGCUnit.makeQueryKey(contact, capability);
            AIGCUnit unit = this.unitMap.get(key);
            if (null != unit) {
                unit.setTalkContext(context);
            }
            else {
                unit = new AIGCUnit(contact, capability, context);
                this.unitMap.put(key, unit);
            }
            result.add(unit);
        }

        return result;
    }

    public List<AIGCUnit> teardownUnit(Contact contact) {
        List<AIGCUnit> result = new ArrayList<>();

        Iterator<AIGCUnit> iter = this.unitMap.values().iterator();
        while (iter.hasNext()) {
            AIGCUnit unit = iter.next();
            if (unit.getContact().getId().equals(contact.getId())) {
                result.add(unit);
                iter.remove();
                this.chatQueueMap.remove(unit.getQueryKey());
                this.conversationQueueMap.remove(unit.getQueryKey());
            }
        }

        return result;
    }

    public List<AIGCUnit> getAllUnits() {
        List<AIGCUnit> list = new ArrayList<>(this.unitMap.size());
        list.addAll(this.unitMap.values());
        return list;
    }

    public List<AIGCChannel> getAllChannels() {
        List<AIGCChannel> list = new ArrayList(this.channelMap.size());
        list.addAll(this.channelMap.values());
        return list;
    }

    public List<ModelConfig> getModelConfigs() {
        if (!this.isStarted()) {
            return null;
        }

        return this.storage.getModelConfigs();
    }

    public List<Notification> getNotifications() {
        if (!this.isStarted()) {
            return null;
        }

        return this.storage.readEnabledNotifications();
    }

    public AIGCUnit selectUnitByName(String unitName) {
        ArrayList<AIGCUnit> candidates = new ArrayList<>();

        Iterator<AIGCUnit> iter = this.unitMap.values().iterator();
        while (iter.hasNext()) {
            AIGCUnit unit = iter.next();
            if (unit.getCapability().getName().equals(unitName) &&
                unit.getContext().isValid()) {
                candidates.add(unit);
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        int num = candidates.size();
        if (num == 1) {
            return candidates.get(0);
        }

        // 先进行一次随机选择
        AIGCUnit unit = candidates.get(Utils.randomInt(0, num - 1));

        iter = candidates.iterator();
        while (iter.hasNext()) {
            AIGCUnit u = iter.next();
            if (u.isRunning()) {
                // 把正在运行的单元从候选列表里删除
                iter.remove();
            }
        }

        if (candidates.isEmpty()) {
            // 所有单元都在运行，返回随机选择
            return unit;
        }

        unit = candidates.get(Utils.randomInt(0, candidates.size() - 1));
        return unit;
    }

    public AIGCUnit selectUnitBySubtask(String subtask) {
        ArrayList<AIGCUnit> candidates = new ArrayList<>();

        Iterator<AIGCUnit> iter = this.unitMap.values().iterator();
        while (iter.hasNext()) {
            AIGCUnit unit = iter.next();
            if (unit.getCapability().containsSubtask(subtask) && unit.getContext().isValid()) {
                candidates.add(unit);
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        int num = candidates.size();
        if (num == 1) {
            return candidates.get(0);
        }

        // 先进行一次随机选择
        AIGCUnit unit = candidates.get(Utils.randomInt(0, num - 1));

        iter = candidates.iterator();
        while (iter.hasNext()) {
            AIGCUnit u = iter.next();
            if (u.isRunning()) {
                // 把正在运行的单元从候选列表里删除
                iter.remove();
            }
        }

        if (candidates.isEmpty()) {
            // 所有单元都在运行，返回随机选择
            return unit;
        }

        unit = candidates.get(Utils.randomInt(0, candidates.size() - 1));
        return unit;
    }

    /**
     * 通过邀请码查询令牌。
     *
     * @param invitationCode
     * @return
     */
    public String queryTokenByInvitation(String invitationCode) {
        return this.storage.readTokenByInvitation(invitationCode);
    }

    /**
     * 为令牌创建新的邀请码。
     *
     * @param token
     * @return
     */
    public String newInvitationForToken(String token) {
        String invitation = Utils.randomNumberString(6);
        if (!this.storage.writeInvitation(invitation, token)) {
            Logger.e(this.getClass(), "#newInvitationForToken - write invitation failed: " + token);
        }
        return invitation;
    }

    /**
     * 检测或注入验证码。
     *
     * @param phoneNumber
     * @param userName 用户名，可以为 {@code null} 值。
     * @return
     */
    public AuthToken getOrInjectAuthToken(String phoneNumber, String userName) {
        long phone = 0;
        try {
            phone = Long.parseLong(phoneNumber);
        } catch (Exception e) {
            Logger.e(this.getClass(), "");
            return null;
        }

        final String domain = "shixincube.com";
        final String appKey = "shixin-cubeteam-opensource-appkey";

        AuthToken authToken = null;

        ContactSearchResult searchResult = ContactManager.getInstance().searchWithContactId(domain, Long.toString(phone));
        if (searchResult.getContactList().isEmpty()) {
            // 没有该联系人
            Contact contact = ContactManager.getInstance().createContact(phone,
                    domain, (null != userName) ? userName : phoneNumber, null);
            // 创建令牌
            AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
            // 5年有效时长
            authToken = authService.applyToken(domain, appKey, contact.getId(), 5L * 365 * 24 * 60 * 60 * 1000);
        }
        else {
            // 有该联系人
            AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
            authToken = authService.queryAuthTokenByContactId(phone);
        }

        return authToken;
    }

    /**
     * 获取令牌。
     *
     * @param tokenCode
     * @return
     */
    public AuthToken getToken(String tokenCode) {
        AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
        AuthToken authToken = authService.getToken(tokenCode);
        return authToken;
    }

    /**
     * 获取对应的知识库实例。
     *
     * @param tokenCode
     * @return
     */
    public synchronized KnowledgeBase getKnowledgeBase(String tokenCode) {
        KnowledgeBase base = this.knowledgeMap.get(tokenCode);
        if (null == base) {
            AuthToken authToken = this.getToken(tokenCode);
            if (null == authToken) {
                return null;
            }

            AbstractModule fileStorage = this.getKernel().getModule("FileStorage");
            if (null == fileStorage) {
                Logger.e(this.getClass(), "#getKnowledgeBase - File storage service is not ready");
                return null;
            }

            base = new KnowledgeBase(this, this.storage, authToken, fileStorage);
            this.knowledgeMap.put(tokenCode, base);
        }
        return base;
    }

    /**
     * 对历史问答进行评价。
     *
     * @param historySN
     * @param scores
     */
    public void evaluate(long historySN, int scores) {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                storage.updateChatHistoryFeedback(historySN, scores);
            }
        });
    }

    /**
     * 获取指定频道。
     *
     * @param channelCode
     * @return
     */
    public AIGCChannel getChannel(String channelCode) {
        return this.channelMap.get(channelCode);
    }

    /**
     * 通过访问令牌获取对应的频道。
     *
     * @param tokenCode
     * @return
     */
    public AIGCChannel getChannelByToken(String tokenCode) {
        for (Map.Entry<String, AIGCChannel> e : this.channelMap.entrySet()) {
            AIGCChannel channel = e.getValue();
            if (channel.getAuthToken().getCode().equals(tokenCode)) {
                return channel;
            }
        }
        return null;
    }

    /**
     * 创建频道。
     *
     * @param token
     * @param participant
     * @param channelCode
     * @return
     */
    public AIGCChannel createChannel(String token, String participant, String channelCode) {
        AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
        AuthToken authToken = authService.getToken(token);
        if (null == authToken) {
            return null;
        }

        AIGCChannel channel = new AIGCChannel(authToken, participant, channelCode);
        this.channelMap.put(channel.getCode(), channel);
        return channel;
    }

    /**
     * 申请频道。
     *
     * @param token
     * @param participant
     * @return
     */
    public AIGCChannel requestChannel(String token, String participant) {
        if (this.channelMap.size() >= this.maxChannel) {
            Logger.w(AIGCService.class, "#requestChannel - Channel num overflow: " + this.maxChannel);
            return null;
        }

        if (!this.checkParticipantName(participant)) {
            Logger.w(AIGCService.class, "#requestChannel - Participant is sensitive word: " + participant);
            return null;
        }

        Iterator<Map.Entry<String, AIGCChannel>> iter = this.channelMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, AIGCChannel> e = iter.next();
            AIGCChannel channel = e.getValue();
            if (channel.getAuthToken().getCode().equals(token)) {
                // 当前频道是否还在工作状态
                if (channel.isProcessing()) {
                    Logger.w(AIGCService.class, "#requestChannel - Channel is processing: " + channel.getCode());
                    return null;
                }
            }
        }

        AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
        AuthToken authToken = authService.getToken(token);

        AIGCChannel channel = new AIGCChannel(authToken, participant);
        this.channelMap.put(channel.getCode(), channel);
        return channel;
    }

    /**
     * 保活频道。
     *
     * @param token
     * @return
     */
    public boolean keepAliveChannel(String token) {
        AIGCChannel channel = this.channelMap.get(token);
        if (null == channel) {
            return false;
        }

        channel.setActiveTimestamp(System.currentTimeMillis());
        return true;
    }

    /**
     * 提交事件。
     *
     * @param event
     * @return
     */
    public EventResult submitEvent(Event event) {
        return Explorer.getInstance().fireEvent(event);
    }

    /**
     * 预推理，以复合上下文形式进行描述。
     *
     * @param token
     * @param content
     * @return
     */
    public ComplexContext preInfer(String token, String content) {
        AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
        AuthToken authToken = authService.getToken(token);
        return this.recognizeContext(content, authToken);
    }

    /**
     * 使用模块进行推理。
     *
     * @param token
     * @param moduleName
     * @param params
     * @return
     */
    public String inferByModule(String token, String moduleName, JSONObject params) {
        AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
        AuthToken authToken = authService.getToken(token);
        if (null == authToken) {
            Logger.w(this.getClass(), "#inferByModule - Auth token error: " + token);
            return null;
        }

        cube.service.aigc.module.Module module = ModuleManager.getInstance().getModule(moduleName);
        if (null == module) {
            Logger.w(this.getClass(), "#inferByModule - Module is not find: " + moduleName);
            return null;
        }

        if (module instanceof PublicOpinion) {
            if (!params.has("task")) {
                Logger.d(this.getClass(), "#inferByModule - PublicOpinion module param error");
                return null;
            }

            String taskName = params.getString("task");
            PublicOpinionTaskName task = PublicOpinionTaskName.parse(taskName);
            if (null == task) {
                Logger.d(this.getClass(), "#inferByModule - PublicOpinion task is unknown: " + taskName);
                return null;
            }

            if (task == PublicOpinionTaskName.ArticleSentimentSummary ||
                task == PublicOpinionTaskName.ArticleSentimentClassification) {
                if (!params.has("category") || !params.has("title")) {
                    Logger.d(this.getClass(), "#inferByModule - PublicOpinion module param error");
                    return null;
                }

                String category = params.getString("category");
                String title = params.getString("title");
                String sentiment = params.has("sentiment") ?
                        params.getString("sentiment") : null;

                PublicOpinion po = (PublicOpinion) module;

                MutableArticleQuery maq = new MutableArticleQuery();
                if (PublicOpinionTaskName.ArticleSentimentSummary == task) {
                    maq.articleQuery = po.makeEvaluatingArticleQuery(category, title,
                            (null != sentiment) ? Sentiment.parse(sentiment) : null);
                }
                else if (PublicOpinionTaskName.ArticleSentimentClassification == task) {
                    maq.articleQuery = po.makeArticleClassificationQuery(category, title);
                }
                if (null == maq.articleQuery) {
                    Logger.w(this.getClass(), "#inferByModule - Make article query failed: " + category);
                    return null;
                }

                AIGCUnit unit = (this.useAgent) ? Agent.getInstance().getUnit()
                        :  this.selectUnitBySubtask(AICapability.NaturalLanguageProcessing.Conversational);
                if (null == unit) {
                    Logger.w(this.getClass(), "#inferByModule - Unit error");
                    return null;
                }

                AIGCChannel channel = this.getChannel(authToken);
                if (null == channel) {
                    channel = this.createChannel(token, "User-" + authToken.getContactId(),
                            Utils.randomString(16));
                }

                StringBuilder result = new StringBuilder();

                this.singleChat(channel, unit, maq.articleQuery.query, null, new ChatListener() {
                    @Override
                    public void onChat(AIGCChannel channel, AIGCGenerationRecord record) {
                        maq.articleQuery.answer = record.answer.replaceAll(",", "，");
                        synchronized (result) {
                            result.append(maq.articleQuery.output());
                            result.notify();
                        }
                    }

                    @Override
                    public void onFailed(AIGCChannel channel) {
                        synchronized (result) {
                            result.notify();
                        }
                    }
                });

                synchronized (result) {
                    try {
                        result.wait(2 * 60 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                return result.toString();
            }
        }

        return null;
    }

    /**
     * 执行聊天任务。
     *
     * @param channelCode
     * @param content
     * @param unitName
     * @param numHistories
     * @param records
     * @param listener
     * @return
     */
    public boolean chat(String channelCode, String content, String unitName, int numHistories,
                        List<AIGCGenerationRecord> records, ChatListener listener) {
        if (!this.isStarted()) {
            Logger.w(AIGCService.class, "#chat - Service is NOT ready");
            return false;
        }

        if (content.length() > this.maxChatContent) {
            Logger.w(AIGCService.class, "#chat - Content length greater than " + this.maxChatContent);
            return false;
        }

        // 获取频道
        AIGCChannel channel = this.channelMap.get(channelCode);
        if (null == channel) {
            Logger.w(AIGCService.class, "#chat - Can NOT find AIGC channel: " + channelCode);
            return false;
        }

        // 如果频道正在应答上一次问题，则返回 null
        if (channel.isProcessing()) {
            Logger.w(AIGCService.class, "#chat - Channel is processing: " + channelCode);
            return false;
        }

        // 设置为正在处理
        channel.setProcessing(true);

        // 查找有该能力的单元
        // 优先按照单元名称进行检索，然后按照描述进行检索
        AIGCUnit unit = null;
        if (this.useAgent) {
            unit = Agent.getInstance().getUnit();
        }
        else {
            if (null != unitName) {
                unit = this.selectUnitByName(unitName);
            }
            else {
                unit = this.selectUnitBySubtask(AICapability.NaturalLanguageProcessing.Conversational);
                if (null == unit) {
                    this.selectUnitBySubtask(AICapability.NaturalLanguageProcessing.ImprovedConversational);
                }
            }
        }

        if (null == unit) {
            Logger.w(AIGCService.class, "#chat - No conversational task unit setup in server");
            channel.setProcessing(false);
            return false;
        }

        ChatUnitMeta meta = new ChatUnitMeta(unit, channel, content, records, listener);
        meta.numHistories = numHistories;

        synchronized (this.chatQueueMap) {
            Queue<ChatUnitMeta> queue = this.chatQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.chatQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            unit.setRunning(true);

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processChatQueue(meta.unit.getQueryKey());
                }
            });
        }

        return true;
    }

    public void singleChat(AIGCChannel channel, AIGCUnit unit, String query, List<AIGCGenerationRecord> records,
                           ChatListener listener) {
        if (this.useAgent) {
            unit = Agent.getInstance().getUnit();
        }

        if (null == unit) {
            // 没有单元数据
            listener.onFailed(channel);
            return;
        }

        ChatUnitMeta meta = new ChatUnitMeta(unit, channel, query, records, listener);

        synchronized (this.chatQueueMap) {
            Queue<ChatUnitMeta> queue = this.chatQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.chatQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            unit.setRunning(true);

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processChatQueue(meta.unit.getQueryKey());
                }
            });
        }
    }

    /**
     * 增强型互动会话。
     *
     * @param code
     * @param content
     * @param parameter
     * @param listener
     * @return
     */
    public boolean conversation(String code, String content, AIGCConversationParameter parameter, ConversationListener listener) {
        if (!this.isStarted()) {
            Logger.w(AIGCService.class, "#conversation - Service is NOT ready");
            return false;
        }

        if (content.length() > this.maxChatContent) {
            Logger.w(AIGCService.class, "#conversation - Content length greater than " + this.maxChatContent);
            return false;
        }

        // 获取频道
        AIGCChannel channel = this.channelMap.get(code);
        if (null == channel) {
            Logger.w(AIGCService.class, "#conversation - Can NOT find AIGC channel: " + code);
            return false;
        }

        // 如果频道正在应答上一次问题，则返回 null
        if (channel.isProcessing()) {
            Logger.w(AIGCService.class, "#conversation - Channel is processing: " + code);
            return false;
        }

        channel.setProcessing(true);

        // 查找有该能力的单元
        AIGCUnit unit = this.selectUnitByName("MOSS");
        if (null == unit) {
            Logger.w(AIGCService.class, "#conversation - No conversational task unit setup in server");
            channel.setProcessing(false);
            return false;
        }

        ConversationUnitMeta meta = new ConversationUnitMeta(unit, channel, content, parameter, listener);

        synchronized (this.conversationQueueMap) {
            Queue<ConversationUnitMeta> queue = this.conversationQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.conversationQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            unit.setRunning(true);

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processConversationQueue(meta.unit.getQueryKey());
                }
            });
        }

        return true;
    }

    public void singleConversation(AIGCChannel channel, AIGCUnit unit, String query, ConversationListener listener) {
        AIGCConversationParameter parameter = new AIGCConversationParameter(0.7f, 0.8f, 1.02f,
                new ArrayList<>());
        ConversationUnitMeta meta = new ConversationUnitMeta(unit, channel, query, parameter, listener);

        synchronized (this.conversationQueueMap) {
            Queue<ConversationUnitMeta> queue = this.conversationQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.conversationQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            unit.setRunning(true);

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processConversationQueue(meta.unit.getQueryKey());
                }
            });
        }
    }

    /**
     * 执行自然语言任务。
     *
     * @param task
     * @param listener
     * @return
     */
    public boolean performNaturalLanguageTask(NLTask task, NaturalLanguageTaskListener listener) {
        if (!this.isStarted()) {
            return false;
        }

        // 检查任务
        if (!task.check()) {
            Logger.w(AIGCService.class, "Natural language task data error: " + task.type);
            return false;
        }

        // 查找有该能力的单元
        AIGCUnit unit = this.selectUnitBySubtask(AICapability.NaturalLanguageProcessing.MultiTask);
        if (null == unit) {
            Logger.w(AIGCService.class, "No natural language task unit setup in server");
            return false;
        }

        NaturalLanguageTaskMeta meta = new NaturalLanguageTaskMeta(unit, task, listener);

        synchronized (this.nlTaskQueueMap) {
            Queue<NaturalLanguageTaskMeta> queue = this.nlTaskQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.nlTaskQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            unit.setRunning(true);

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processNaturalLanguageTaskQueue(meta.unit.getQueryKey());
                }
            });
        }

        return true;
    }

    public boolean sentimentAnalysis(String text, SentimentAnalysisListener listener) {
        if (!this.isStarted()) {
            return false;
        }

        // 查找有该能力的单元
        AIGCUnit unit = this.selectUnitBySubtask(AICapability.NaturalLanguageProcessing.SentimentAnalysis);
        if (null == unit) {
            Logger.w(AIGCService.class, "No sentiment analysis unit setup in server");
            return false;
        }

        SentimentUnitMeta meta = new SentimentUnitMeta(unit, text, listener);

        synchronized (this.sentimentQueueMap) {
            Queue<SentimentUnitMeta> queue = this.sentimentQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.sentimentQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            unit.setRunning(true);

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processSentimentQueue(meta.unit.getQueryKey());
                }
            });
        }

        return true;
    }

    /**
     * 生成文本摘要。
     *
     * @param text
     * @param listener
     * @return
     */
    public boolean generateSummarization(String text, SummarizationListener listener) {
        if (!this.isStarted()) {
            return false;
        }

        // 查找有该能力的单元
        AIGCUnit unit = this.selectUnitBySubtask(AICapability.NaturalLanguageProcessing.Summarization);
        if (null == unit) {
            Logger.w(AIGCService.class, "No summarization unit setup in server");
            return false;
        }

        SummarizationUnitMeta meta = new SummarizationUnitMeta(unit, text, listener);

        synchronized (this.summarizationQueueMap) {
            Queue<SummarizationUnitMeta> queue = this.summarizationQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.summarizationQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            unit.setRunning(true);

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processSummarizationQueue(meta.unit.getQueryKey());
                }
            });
        }

        return true;
    }

    /**
     * 文本生成图片。
     *
     * @param channelCode
     * @param text
     * @param unitName
     * @param listener
     * @return
     */
    public boolean generateImage(String channelCode, String text, String unitName, TextToImageListener listener) {
        if (!this.isStarted()) {
            return false;
        }

        // 获取频道
        AIGCChannel channel = this.channelMap.get(channelCode);
        if (null == channel) {
            Logger.w(AIGCService.class, "#generateImage - Can NOT find AIGC channel: " + channelCode);
            return false;
        }

        return this.generateImage(channel, text, unitName, listener);
    }

    /**
     * 文本生成图片。
     *
     * @param channel
     * @param text
     * @param unitName
     * @param listener
     * @return
     */
    public boolean generateImage(AIGCChannel channel, String text, String unitName, TextToImageListener listener) {
        if (!this.isStarted()) {
            return false;
        }

        // 如果频道正在应答上一次问题，则返回 null
        if (channel.isProcessing()) {
            Logger.w(AIGCService.class, "#generateImage - Channel is processing: " + channel.getCode());
            return false;
        }

        // 设置为正在处理
        channel.setProcessing(true);

        // 查找有该能力的单元
        AIGCUnit unit = this.selectUnitByName(unitName);
        if (null == unit || !ModelConfig.isTextToImageUnit(unitName)) {
            Logger.w(AIGCService.class, "No text to image unit: " + unitName);
            channel.setProcessing(false);
            return false;
        }

        unit = this.selectUnitBySubtask(AICapability.Multimodal.TextToImage);
        if (null == unit) {
            Logger.w(AIGCService.class, "No text to image unit setup in server");
            channel.setProcessing(false);
            return false;
        }

        TextToImageUnitMeta meta = new TextToImageUnitMeta(unit, channel, text, listener);

        synchronized (this.textToImageQueueMap) {
            Queue<TextToImageUnitMeta> queue = this.textToImageQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.textToImageQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            unit.setRunning(true);

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processTextToImageQueue(meta.unit.getQueryKey());
                }
            });
        }

        return true;
    }

    /**
     * 提取关键词。
     *
     * @param text
     * @param listener
     * @return
     */
    public boolean extractKeywords(String text, ExtractKeywordsListener listener) {
        if (!this.isStarted()) {
            return false;
        }

        // 查找有该能力的单元
        AIGCUnit unit = this.selectUnitBySubtask(AICapability.NaturalLanguageProcessing.ExtractKeywords);
        if (null == unit) {
            Logger.w(AIGCService.class, "No extract keywords unit setup in server");
            return false;
        }

        ExtractKeywordsUnitMeta meta = new ExtractKeywordsUnitMeta(unit, text, listener);

        synchronized (this.extractKeywordsQueueMap) {
            Queue<ExtractKeywordsUnitMeta> queue = this.extractKeywordsQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.extractKeywordsQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            unit.setRunning(true);

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processExtractKeywordsQueue(meta.unit.getQueryKey());
                }
            });
        }

        return true;
    }

    /**
     * 心理学绘画预测。
     *
     * @param token
     * @param fileCode
     * @return
     */
    public PsychologyReport generatePsychologyReport(String token, String fileCode) {
        if (!this.isStarted()) {
            return null;
        }

        AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
        AuthToken authToken = authService.getToken(token);
        if (null == authToken) {
            Logger.w(this.getClass(), "#generatePsychologyReport - Token error: " + token);
            return null;
        }

        AbstractModule fileStorage = this.getKernel().getModule("FileStorage");
        if (null == fileStorage) {
            Logger.e(this.getClass(), "#generatePsychologyReport - File storage service is not ready");
            return null;
        }

        GetFile getFile = new GetFile(authToken.getDomain(), fileCode);
        JSONObject fileLabelJson = fileStorage.notify(getFile);
        if (null == fileLabelJson) {
            Logger.e(this.getClass(), "#generatePsychologyReport - Get file failed: " + fileCode);
            return null;
        }

        FileLabel fileLabel = new FileLabel(fileLabelJson);

        PsychologyReport report = PsychologyScene.getInstance().generateEvaluationReport(
                this.getChannelByToken(token), fileLabel, new SceneListener() {
            @Override
            public void onPaintingPredictCompleted(PsychologyReport report, Painting painting) {

            }

            @Override
            public void onPaintingPredictFailed(PsychologyReport report) {

            }

            @Override
            public void onReportEvaluated(PsychologyReport report) {

            }

            @Override
            public void onReportEvaluateFailed(PsychologyReport report) {

            }
        });
        return report;
    }

    public boolean automaticSpeechRecognition(String domain, String fileCode, AutomaticSpeechRecognitionListener listener) {
        AbstractModule fileStorage = this.getKernel().getModule("FileStorage");
        if (null == fileStorage) {
            Logger.e(this.getClass(), "#automaticSpeechRecognition - File storage service is not ready");
            return false;
        }

        GetFile getFile = new GetFile(domain, fileCode);
        JSONObject fileLabelJson = fileStorage.notify(getFile);
        if (null == fileLabelJson) {
            Logger.e(this.getClass(), "#automaticSpeechRecognition - Get file failed: " + fileCode);
            return false;
        }

        FileLabel sourceFile = new FileLabel(fileLabelJson);

        FileLabel fileLabel = new FileLabel(fileLabelJson);
        boolean deleteFileLabel = false;

        AbstractModule fileProcessor = this.getKernel().getModule("FileProcessor");
        if (null == fileProcessor) {
            Logger.e(this.getClass(), "#automaticSpeechRecognition - File processor service is not ready");
            return false;
        }

        if (FileUtils.isVideoType(fileLabel.getFileType())) {
            // 从视频文件中提取音频文件
            ExtractAudioOperation audioOperation = new ExtractAudioOperation(FileType.WAV);
            JSONObject processor = new JSONObject();
            processor.put("action", FileProcessorAction.Video.name);
            processor.put("domain", fileLabel.getDomain().getName());
            processor.put("fileCode", fileLabel.getFileCode());
            processor.put("parameter", audioOperation.toJSON());

            JSONObject resultJson = fileProcessor.notify(processor);
            if (null == resultJson) {
                Logger.e(this.getClass(), "#automaticSpeechRecognition - Extract audio operation result is NULL: "
                    + fileLabel.getFileCode());
                return false;
            }

            FileProcessResult result = new FileProcessResult(resultJson);
            if (!result.success) {
                Logger.e(this.getClass(), "#automaticSpeechRecognition - Extract audio operation result error: "
                        + fileLabel.getFileCode());
                return false;
            }

            // 音频文件
            File audioFile = result.getResultList().get(0).file;
            String audioFileCode = FileUtils.makeFileCode(fileLabel.getOwnerId(), fileLabel.getDomain().getName(), audioFile.getName());
            FileLabel audioFileLabel = FileUtils.makeFileLabel(fileLabel.getDomain().getName(),
                    audioFileCode, fileLabel.getOwnerId(), audioFile);
            // 将处理后文件存入存储器
            JSONObject saveFile = new JSONObject();
            saveFile.put("action", FileStorageAction.SaveFile.name);
            saveFile.put("path", audioFile.getAbsolutePath());
            saveFile.put("fileLabel", audioFileLabel.toJSON());
            JSONObject audioFileLabelJson = fileStorage.notify(saveFile);
            if (null == audioFileLabelJson) {
                Logger.e(this.getClass(), "#automaticSpeechRecognition - Save audio file to storage failed");
                return false;
            }

            // 更新文件标签
            fileLabel = new FileLabel(audioFileLabelJson);
            // 在处理后删除该音频数据
            deleteFileLabel = true;
        }
        else if (!FileUtils.isAudioType(fileLabel.getFileType())) {
            Logger.e(this.getClass(), "#automaticSpeechRecognition - File type is NOT audio type: "
                    + fileLabel.getFileCode());
            return false;
        }

        // 音频重采用
        /*AudioSamplingOperation samplingOperation = new AudioSamplingOperation(1, 16000, FileType.WAV);

        JSONObject processor = new JSONObject();
        processor.put("action", FileProcessorAction.Audio.name);
        processor.put("domain", fileLabel.getDomain().getName());
        processor.put("fileCode", fileLabel.getFileCode());
        processor.put("parameter", samplingOperation.toJSON());

        JSONObject resultJson = fileProcessor.notify(processor);
        if (null == resultJson) {
            Logger.e(this.getClass(), "#automaticSpeechRecognition - File processor result is NULL");
            return false;
        }*/

        // 音频文件分割
        AudioCropOperation cropOperation = new AudioCropOperation(0, 30, FileType.WAV);
        JSONObject processor = new JSONObject();
        processor.put("action", FileProcessorAction.Audio.name);
        processor.put("domain", fileLabel.getDomain().getName());
        processor.put("fileCode", fileLabel.getFileCode());
        processor.put("parameter", cropOperation.toJSON());

        JSONObject resultJson = fileProcessor.notify(processor);
        if (null == resultJson) {
            Logger.e(this.getClass(), "#automaticSpeechRecognition - Audio crop result is NULL: "
                    + fileLabel.getFileCode());
            if (deleteFileLabel) {
                JSONObject deleteFile = new JSONObject();
                deleteFile.put("action", FileStorageAction.DeleteFile.name);
                deleteFile.put("domain", fileLabel.getDomain().getName());
                deleteFile.put("fileCode", fileLabel.getFileCode());
                fileStorage.notify(deleteFile);
            }
            return false;
        }

        FileProcessResult result = new FileProcessResult(resultJson);
        if (null == result.getAudioResult()) {
            Logger.e(this.getClass(), "#automaticSpeechRecognition - Audio crop result error: "
                    + fileLabel.getFileCode());
            if (deleteFileLabel) {
                JSONObject deleteFile = new JSONObject();
                deleteFile.put("action", FileStorageAction.DeleteFile.name);
                deleteFile.put("domain", fileLabel.getDomain().getName());
                deleteFile.put("fileCode", fileLabel.getFileCode());
                fileStorage.notify(deleteFile);
            }
            return false;
        }

        File resultFile = result.getResultList().get(0).file;
        String localFileCode = FileUtils.makeFileCode(fileLabel.getOwnerId(), fileLabel.getDomain().getName(), resultFile.getName());
        FileLabel localFileLabel = FileUtils.makeFileLabel(fileLabel.getDomain().getName(),
                localFileCode, fileLabel.getOwnerId(), resultFile);
        // 将处理后文件存入存储器
        JSONObject saveFile = new JSONObject();
        saveFile.put("action", FileStorageAction.SaveFile.name);
        saveFile.put("path", resultFile.getAbsolutePath());
        saveFile.put("fileLabel", localFileLabel.toJSON());
        JSONObject localFileLabelJson = fileStorage.notify(saveFile);
        if (null == localFileLabelJson) {
            Logger.e(this.getClass(), "#automaticSpeechRecognition - Save file to storage failed");
            if (deleteFileLabel) {
                JSONObject deleteFile = new JSONObject();
                deleteFile.put("action", FileStorageAction.DeleteFile.name);
                deleteFile.put("domain", fileLabel.getDomain().getName());
                deleteFile.put("fileCode", fileLabel.getFileCode());
                fileStorage.notify(deleteFile);
            }
            return false;
        }

        // 文件标签
        localFileLabel = new FileLabel(localFileLabelJson);

        // 请求 AIGC 单元
        // 查找有该能力的单元
        AIGCUnit unit = this.selectUnitBySubtask(AICapability.AudioProcessing.AutomaticSpeechRecognition);
        if (null == unit) {
            Logger.w(AIGCService.class, "No ASR task unit setup in server");
            if (deleteFileLabel) {
                JSONObject deleteFile = new JSONObject();
                deleteFile.put("action", FileStorageAction.DeleteFile.name);
                deleteFile.put("domain", fileLabel.getDomain().getName());
                deleteFile.put("fileCode", fileLabel.getFileCode());
                fileStorage.notify(deleteFile);
            }
            return false;
        }

        ASRUnitMeta meta = new ASRUnitMeta(unit, sourceFile, localFileLabel, listener);

        synchronized (this.asrQueueMap) {
            Queue<ASRUnitMeta> queue = this.asrQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.asrQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            unit.setRunning(true);

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processASRQueue(meta.unit.getQueryKey());
                }
            });
        }

        if (deleteFileLabel) {
            JSONObject deleteFile = new JSONObject();
            deleteFile.put("action", FileStorageAction.DeleteFile.name);
            deleteFile.put("domain", fileLabel.getDomain().getName());
            deleteFile.put("fileCode", fileLabel.getFileCode());
            fileStorage.notify(deleteFile);
        }

        return true;
    }

    /**
     * 文本分词。
     *
     * @param text
     * @return
     */
    public List<String> segmentation(String text) {
        return this.tokenizer.sentenceProcess(text);
    }

    /**
     * 执行命令。
     *
     * @param command
     * @param listener
     */
    public void executeCommand(final Command command, final CommandListener listener) {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                command.run();
            }
        });
    }

    /**
     * 计算文本的 Token 列表。
     *
     * @param text
     * @return
     */
    public List<String> calcTokens(String text) {
        List<String> tokens = this.tokenizer.sentenceProcess(text);
        tokens.removeIf(s -> !TextUtils.isChineseWord(s) && !TextUtils.isWord(s));
        return tokens;
    }

    /**
     * 对聊天内容进行分类识别。
     *
     * @param text
     * @param authToken
     * @return
     */
    private ComplexContext recognizeContext(String text, AuthToken authToken) {
        final String content = text.trim();
        ComplexContext result = new ComplexContext(ComplexContext.Type.Simplex);

        List<String> urlList = TextUtils.extractAllURLs(content);
        if (!urlList.isEmpty()) {
            // 内容包含 URL 链接
            AIGCUnit unit = this.selectUnitBySubtask(AICapability.DataProcessing.ExtractURLContent);
            if (null == unit) {
                Logger.w(this.getClass(), "#recognizeContent - Can NOT find extract URL content unit");
                return result;
            }

            // 对 URL 进行数据读取
            JSONArray urlArray = new JSONArray();
            for (String url : urlList) {
                urlArray.put(url);
            }
            JSONObject payload = new JSONObject();
            payload.put("urls", urlArray);
            Packet request = new Packet(AIGCAction.ExtractURLContent.name, payload);
            ActionDialect dialect = this.cellet.transmit(unit.getContext(), request.toDialect(), 60 * 1000);
            if (null == dialect) {
                Logger.w(this.getClass(), "#recognizeContent - Unit is error");
                return result;
            }

            Packet response = new Packet(dialect);
            if (Packet.extractCode(response) != AIGCStateCode.Ok.code) {
                Logger.d(this.getClass(), "#recognizeContent - Process url list failed");
                result = new ComplexContext(ComplexContext.Type.Complex);
                for (String url : urlList) {
                    HyperlinkResource resource = new HyperlinkResource(url, HyperlinkResource.TYPE_FAILURE);
                    result.addResource(resource);
                }
            }
            else {
                JSONObject data = Packet.extractDataPayload(response);
                JSONArray list = data.getJSONArray("list");
                result = new ComplexContext(ComplexContext.Type.Complex);
                for (int i = 0; i < list.length(); ++i) {
                    JSONObject resPayload = new JSONObject();
                    resPayload.put("payload", list.getJSONObject(i));
                    HyperlinkResource resource = new HyperlinkResource(resPayload);
                    result.addResource(resource);
                }
            }
        }
        else if (TextUtils.isURL(content)) {
            AIGCUnit unit = this.selectUnitBySubtask(AICapability.DataProcessing.ExtractURLContent);
            if (null == unit) {
                Logger.w(this.getClass(), "#recognizeContent - Can NOT find extract URL content unit");
                return result;
            }

            // 对 URL 进行数据读取
            JSONObject payload = new JSONObject();
            payload.put("url", content);
            Packet request = new Packet(AIGCAction.ExtractURLContent.name, payload);
            ActionDialect dialect = this.cellet.transmit(unit.getContext(), request.toDialect(), 60 * 1000);
            if (null == dialect) {
                Logger.w(this.getClass(), "#recognizeContent - Unit is error");
                return result;
            }

            Packet response = new Packet(dialect);
            if (Packet.extractCode(response) != AIGCStateCode.Ok.code) {
                HyperlinkResource resource = new HyperlinkResource(content, HyperlinkResource.TYPE_FAILURE);
                result = new ComplexContext(ComplexContext.Type.Complex);
                result.addResource(resource);
            }
            else {
                JSONObject data = Packet.extractDataPayload(response);
                JSONArray list = data.getJSONArray("list");
                if (list.isEmpty()) {
                    // 列表没有数据，获取 URL 失败
                    result = new ComplexContext(ComplexContext.Type.Complex);
                    HyperlinkResource resource = new HyperlinkResource(content, HyperlinkResource.TYPE_FAILURE);
                    result.addResource(resource);
                }
                else {
                    result = new ComplexContext(ComplexContext.Type.Complex);
                    JSONObject resPayload = new JSONObject();
                    resPayload.put("payload", list.getJSONObject(0));
                    HyperlinkResource resource = new HyperlinkResource(resPayload);
                    result.addResource(resource);
                }
            }
        }
        else {
            Stage stage = Explorer.getInstance().infer(content);

            if (stage.isComplex()) {
                result = new ComplexContext(ComplexContext.Type.Complex);

                if (!stage.chartResources.isEmpty()) {
                    for (ChartResource chartResource : stage.chartResources) {
                        result.addResource(chartResource);
                    }
                }

                if (!stage.attachmentResources.isEmpty()) {
                    for (AttachmentResource attachmentResource : stage.attachmentResources) {
                        result.addResource(attachmentResource);
                    }
                }

                if (stage.inference) {
                    Logger.d(this.getClass(), "#recognizeContext - perform stage");

                    result.setInferable(true);
                    result.setInferring(true);

                    // 上下文 ID
                    final long ctxId = result.getId();

                    stage.perform(this, getChannel(authToken), new StageListener() {
                        @Override
                        public void onPerform(Stage stage, cube.service.aigc.module.Module module,
                                              List<String> answerList) {
                            ComplexContext ctx = Explorer.getInstance().getComplexContext(ctxId);
                            if (null != ctx) {
                                for (String answer : answerList) {
                                    ctx.addInferenceResult(answer);
                                }
                                ctx.setInferring(false);
                            }
                        }
                    });
                }
            }
        }

        return result;
    }

    private AIGCChannel getChannel(AuthToken authToken) {
        Iterator<AIGCChannel> iter = this.channelMap.values().iterator();
        while (iter.hasNext()) {
            AIGCChannel channel = iter.next();
            if (channel.getAuthToken().getCode().equals(authToken.getCode())) {
                return channel;
            }
        }

        // 没有频道，如果使用代理则新建频道
        if (this.useAgent) {
            AIGCChannel channel = new AIGCChannel(authToken, "User-" + authToken.getContactId());
            this.channelMap.put(channel.getCode(), channel);
            return channel;
        }

        return null;
    }

    private void processChatQueue(String queryKey) {
        Queue<ChatUnitMeta> queue = this.chatQueueMap.get(queryKey);
        if (null == queue) {
            Logger.w(AIGCService.class, "#processChatQueue - Not found unit: " + queryKey);
            return;
        }

        ChatUnitMeta meta = queue.poll();
        while (null != meta) {
            // 执行处理
            try {
                meta.process();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#processChatQueue - meta process", e);
            }

            meta = queue.poll();
        }

        AIGCUnit unit = this.unitMap.get(queryKey);
        if (null != unit) {
            unit.setRunning(false);
        }
    }

    private void processConversationQueue(String queryKey) {
        Queue<ConversationUnitMeta> queue = this.conversationQueueMap.get(queryKey);
        if (null == queue) {
            Logger.w(AIGCService.class, "Not found unit: " + queryKey);
            return;
        }

        ConversationUnitMeta meta = queue.poll();
        while (null != meta) {
            // 执行处理
            try {
                meta.process();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#processConversationQueue - meta process", e);
            }

            meta = queue.poll();
        }

        AIGCUnit unit = this.unitMap.get(queryKey);
        if (null != unit) {
            unit.setRunning(false);
        }
    }

    private void processNaturalLanguageTaskQueue(String queryKey) {
        Queue<NaturalLanguageTaskMeta> queue = this.nlTaskQueueMap.get(queryKey);
        if (null == queue) {
            Logger.w(AIGCService.class, "No found unit: " + queryKey);
            return;
        }

        NaturalLanguageTaskMeta meta = queue.poll();
        while (null != meta) {
            // 执行处理
            try {
                meta.process();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#processNaturalLanguageTaskQueue - meta process", e);
            }

            meta = queue.poll();
        }

        AIGCUnit unit = this.unitMap.get(queryKey);
        if (null != unit) {
            unit.setRunning(false);
        }
    }

    private void processSentimentQueue(String queryKey) {
        Queue<SentimentUnitMeta> queue = this.sentimentQueueMap.get(queryKey);
        if (null == queue) {
            Logger.w(AIGCService.class, "#processSentimentQueue - No found unit: " + queryKey);
            return;
        }

        SentimentUnitMeta meta = queue.poll();
        while (null != meta) {
            // 执行处理
            try {
                meta.process();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#processSentimentQueue - meta process", e);
            }

            meta = queue.poll();
        }

        AIGCUnit unit = this.unitMap.get(queryKey);
        if (null != unit) {
            unit.setRunning(false);
        }
    }

    private void processSummarizationQueue(String queryKey) {
        Queue<SummarizationUnitMeta> queue = this.summarizationQueueMap.get(queryKey);
        if (null == queue) {
            Logger.w(AIGCService.class, "#processSummarizationQueue - No found unit: " + queryKey);
            return;
        }

        SummarizationUnitMeta meta = queue.poll();
        while (null != meta) {
            // 执行处理
            try {
                meta.process();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#processSummarizationQueue - meta process", e);
            }

            meta = queue.poll();
        }

        AIGCUnit unit = this.unitMap.get(queryKey);
        if (null != unit) {
            unit.setRunning(false);
        }
    }

    private void processTextToImageQueue(String queryKey) {
        Queue<TextToImageUnitMeta> queue = this.textToImageQueueMap.get(queryKey);
        if (null == queue) {
            Logger.w(AIGCService.class, "#processTextToImageQueue - No found unit: " + queryKey);
            return;
        }

        TextToImageUnitMeta meta = queue.poll();
        while (null != meta) {
            // 执行处理
            try {
                meta.process();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#processTextToImageQueue - meta process", e);
            }

            meta = queue.poll();
        }

        AIGCUnit unit = this.unitMap.get(queryKey);
        if (null != unit) {
            unit.setRunning(false);
        }
    }

    private void processExtractKeywordsQueue(String queryKey) {
        Queue<ExtractKeywordsUnitMeta> queue = this.extractKeywordsQueueMap.get(queryKey);
        if (null == queue) {
            Logger.w(AIGCService.class, "#processExtractKeywordsQueue - No found unit: " + queryKey);
            return;
        }

        ExtractKeywordsUnitMeta meta = queue.poll();
        while (null != meta) {
            // 执行处理
            try {
                meta.process();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#processExtractKeywordsQueue - meta process", e);
            }

            meta = queue.poll();
        }

        AIGCUnit unit = this.unitMap.get(queryKey);
        if (null != unit) {
            unit.setRunning(false);
        }
    }

    private void processASRQueue(String queryKey) {
        Queue<ASRUnitMeta> queue = this.asrQueueMap.get(queryKey);
        if (null == queue) {
            Logger.w(AIGCService.class, "No found unit: " + queryKey);
            return;
        }

        ASRUnitMeta meta = queue.poll();
        while (null != meta) {
            // 执行处理
            try {
                meta.process();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#processASRQueue - meta process", e);
            }

            meta = queue.poll();
        }

        AIGCUnit unit = this.unitMap.get(queryKey);
        if (null != unit) {
            unit.setRunning(false);
        }
    }

    private boolean checkParticipantName(String name) {
        if (name.equalsIgnoreCase("AIGC") || name.equalsIgnoreCase("Cube") ||
            name.equalsIgnoreCase("Baize") || name.equalsIgnoreCase("白泽") ||
            name.equalsIgnoreCase("时信魔方")) {
            return false;
        }
        else {
            return true;
        }
    }


    private class ChatUnitMeta {

        protected final long sn;

        protected AIGCUnit unit;

        protected AIGCChannel channel;

        protected String content;

        protected List<AIGCGenerationRecord> records;

        protected int numHistories;

        protected ChatListener listener;

        protected AIGCChatHistory history;

        public ChatUnitMeta(AIGCUnit unit, AIGCChannel channel, String content,
                            List<AIGCGenerationRecord> records, ChatListener listener) {
            this.sn = Utils.generateSerialNumber();
            this.unit = unit;
            this.channel = channel;
            this.content = content;
            this.records = records;
            this.numHistories = (null != records) ? records.size() : 0;
            this.listener = listener;

            this.history = new AIGCChatHistory(this.sn, unit.getCapability().getName());
            this.history.queryContactId = channel.getAuthToken().getContactId();
            this.history.queryTime = System.currentTimeMillis();
            this.history.queryContent = content;
        }

        public ChatUnitMeta(AIGCUnit unit, AIGCChannel channel, String content, int numHistories, ChatListener listener) {
            this.sn = Utils.generateSerialNumber();
            this.unit = unit;
            this.channel = channel;
            this.content = content;
            this.numHistories = numHistories;
            this.listener = listener;

            this.history = new AIGCChatHistory(this.sn, unit.getCapability().getName());
            this.history.queryContactId = channel.getAuthToken().getContactId();
            this.history.queryTime = System.currentTimeMillis();
            this.history.queryContent = content;
        }

        public void process() {
            // 识别内容
            ComplexContext complexContext = recognizeContext(this.content, this.channel.getAuthToken());

            AIGCGenerationRecord result = null;

            if (complexContext.isSimplex()) {
                // 一般文本

                int maxHistories = 10;
                if (this.unit.getCapability().getName().equalsIgnoreCase("Chat")) {
                    maxHistories = 5;
                }

                JSONObject data = new JSONObject();
                data.put("unit", this.unit.getCapability().getName());
                data.put("content", this.content);

                if (null == this.records) {
                    if (this.numHistories > 0) {
                        List<AIGCGenerationRecord> records = this.channel.getLastHistory(this.numHistories);
                        JSONArray array = new JSONArray();
                        for (AIGCGenerationRecord record : records) {
                            array.put(record.toJSON());
                            if (array.length() >= maxHistories) {
                                break;
                            }
                        }
                        data.put("history", array);
                    }
                    else {
                        data.put("history", new JSONArray());
                    }
                }
                else {
                    JSONArray history = new JSONArray();
                    for (AIGCGenerationRecord record : this.records) {
                        history.put(record.toJSON());
                        if (history.length() >= maxHistories) {
                            break;
                        }
                    }
                    data.put("history", history);
                }

                if (useAgent) {
                    String responseText = Agent.getInstance().chat(channel.getAuthToken().getCode(),
                            channel.getCode(), this.content);
                    if (null != responseText) {
                        // 过滤中文字符
                        responseText = this.filterChinese(this.unit, responseText);
                        result = this.channel.appendRecord(this.content, responseText, complexContext);
                    }
                    else {
                        this.channel.setProcessing(false);
                        // 回调失败
                        this.listener.onFailed(this.channel);
                        return;
                    }
                }
                else {
                    Packet request = new Packet(AIGCAction.Chat.name, data);
                    ActionDialect dialect = cellet.transmit(this.unit.getContext(), request.toDialect());
                    if (null == dialect) {
                        Logger.w(AIGCService.class, "Chat unit error - channel: " + this.channel.getCode());
                        this.channel.setProcessing(false);
                        // 回调错误
                        this.listener.onFailed(this.channel);
                        return;
                    }

                    Packet response = new Packet(dialect);
                    JSONObject payload = Packet.extractDataPayload(response);

                    if (!payload.has("response")) {
                        Logger.w(AIGCService.class, "Chat unit respond failed - channel: " + this.channel.getCode());
                        this.channel.setProcessing(false);
                        // 回调错误
                        this.listener.onFailed(this.channel);
                        return;
                    }

                    String responseText = payload.getString("response");

                    // 过滤中文字符
                    responseText = this.filterChinese(this.unit, responseText);
                    result = this.channel.appendRecord(this.content, responseText, complexContext);
                }
            }
            else {
                // 复合型数据
                ResourceAnswer resourceAnswer = new ResourceAnswer(complexContext);
                String answer = resourceAnswer.answer();
                result = this.channel.appendRecord(this.content, answer, complexContext);
            }

            if (!complexContext.isSimplex()) {
                // 缓存上下文
                Explorer.getInstance().cacheComplexContext(complexContext);
            }

            // 设置 SN
            result.sn = this.sn;

            this.history.answerContactId = unit.getContact().getId();
            this.history.answerTime = System.currentTimeMillis();
            this.history.answerContent = result.answer;

            // 重置状态位
            this.channel.setProcessing(false);

            this.listener.onChat(this.channel, result);

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    // 更新用量
                    List<String> tokens = calcTokens(history.queryContent);
                    long promptTokens = tokens.size();
                    tokens = calcTokens(history.answerContent);
                    long completionTokens = tokens.size();
                    storage.updateUsage(history.queryContactId, completionTokens, promptTokens);

                    // 保存历史记录
                    storage.writeChatHistory(history);

                    if (complexContext.isSimplex()) {
                        // 进行资源搜索
                        SearchResult searchResult = Explorer.getInstance().search(content,
                                history.answerContent, complexContext);
                        if (searchResult.hasResult()) {
                            // 缓存结果，以便客户端读取数据
                            Explorer.getInstance().cacheSearchResult(channel.getAuthToken(),
                                    searchResult);
                        }
                    }
                }
            });
        }

        private String filterChinese(AIGCUnit unit, String text) {
            if (unit.getCapability().getName().equalsIgnoreCase("Chat")) {
                if (TextUtils.containsChinese(text)) {
                    return text.replaceAll(",", "，")
                            .replaceAll(":", "：");
                }
                else {
                    return text;
                }
            }
            else {
                return text;
            }
        }
    }

    private class ConversationUnitMeta {

        protected final long sn;

        protected AIGCUnit unit;

        protected AIGCChannel channel;

        protected String content;

        protected AIGCConversationParameter parameter;

        protected ConversationListener listener;

        protected AIGCChatHistory history;

        public ConversationUnitMeta(AIGCUnit unit, AIGCChannel channel, String content,
                                    AIGCConversationParameter parameter, ConversationListener listener) {
            this.sn = Utils.generateSerialNumber();
            this.unit = unit;
            this.channel = channel;
            this.content = content;
            this.parameter = parameter;
            this.listener = listener;

            this.history = new AIGCChatHistory(this.sn, unit.getCapability().getName());
            this.history.queryContactId = channel.getAuthToken().getContactId();
            this.history.queryTime = System.currentTimeMillis();
            this.history.queryContent = content;
        }

        public void process() {
            ComplexContext complexContext = recognizeContext(this.content, this.channel.getAuthToken());

            JSONObject data = new JSONObject();
            data.put("unit", this.unit.getCapability().getName());
            data.put("content", this.content);
            data.put("temperature", this.parameter.temperature);
            data.put("topP", this.parameter.topP);
            data.put("repetitionPenalty", this.parameter.repetitionPenalty);

            int totalLength = this.content.length();

            if (null != this.parameter.records) {
                JSONArray history = new JSONArray();
                for (AIGCGenerationRecord record : this.parameter.records) {
                    history.put(record.toJSON());
                    // 字数
                    totalLength += record.totalWords();
                }
                data.put("history", history);
            }
            else {
                List<AIGCConversationResponse> responseList = this.channel.extractConversationResponses();
                JSONArray history = new JSONArray();
                for (AIGCConversationResponse response : responseList) {
                    AIGCGenerationRecord record = response.toRecord();
                    history.put(record.toJSON());
                    // 字数
                    totalLength += record.totalWords();
                }
                data.put("history", history);
            }

            // 判断长度
            if (totalLength > maxChatContent + maxChatContent) {
                // 总长度越界
                this.channel.setProcessing(false);
                this.listener.onFailed(this.channel, AIGCStateCode.ContentLengthOverflow.code);
                return;
            }

            Packet request = new Packet(AIGCAction.Conversation.name, data);
            ActionDialect dialect = cellet.transmit(this.unit.getContext(), request.toDialect());
            if (null == dialect) {
                Logger.w(AIGCService.class, "Conversation unit error - channel: " + this.channel.getCode());
                this.channel.setProcessing(false);
                // 回调错误
                this.listener.onFailed(this.channel, AIGCStateCode.UnitError.code);
                return;
            }

            Packet response = new Packet(dialect);
            int stateCode = Packet.extractCode(response);
            if (stateCode != AIGCStateCode.Ok.code) {
                Logger.w(AIGCService.class, "Conversation unit respond failed - channel: " + this.channel.getCode());
                this.channel.setProcessing(false);
                // 回调错误
                this.listener.onFailed(this.channel, stateCode);
                return;
            }

            JSONObject payload = Packet.extractDataPayload(response);
            AIGCConversationResponse convResponse = new AIGCConversationResponse(this.sn, this.content,
                    complexContext, payload);

            // 过滤中文字符
            convResponse.answer = this.filterChinese(convResponse.answer);

            this.history.answerContactId = this.unit.getContact().getId();
            this.history.answerTime = System.currentTimeMillis();
            this.history.answerContent = convResponse.answer;

            // 更新字数
            this.unit.setTotalQueryWords(this.unit.getTotalQueryWords() + this.content.length());

            // 记录
            this.channel.appendRecord(convResponse);

            // 重置状态位
            this.channel.setProcessing(false);

            this.listener.onConversation(this.channel, convResponse);

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    storage.writeChatHistory(history);
                }
            });
        }

        private String filterChinese(String text) {
            if (!TextUtils.containsChinese(text)) {
                return text;
            }

            return text.replaceAll(",", "，")
                    .replaceAll(":", "：");
        }
    }

    private class NaturalLanguageTaskMeta {

        protected AIGCUnit unit;

        protected NLTask task;

        protected NaturalLanguageTaskListener listener;

        public NaturalLanguageTaskMeta(AIGCUnit unit, NLTask task, NaturalLanguageTaskListener listener) {
            this.unit = unit;
            this.task = task;
            this.listener = listener;
        }

        public void process() {
            JSONObject data = new JSONObject();
            data.put("task", this.task.toJSON());
            Packet request = new Packet(AIGCAction.NaturalLanguageTask.name, data);
            ActionDialect dialect = cellet.transmit(this.unit.getContext(), request.toDialect());
            if (null == dialect) {
                Logger.w(AIGCService.class, "Natural language task unit error");
                // 回调错误
                this.listener.onFailed();
                return;
            }

            Packet response = new Packet(dialect);
            JSONObject payload = Packet.extractDataPayload(response);

            String type = payload.getString("task");
            JSONArray resultList = payload.getJSONArray("result");

            this.listener.onCompleted(new NLTask(type, resultList));
        }
    }

    private class SentimentUnitMeta {

        protected AIGCUnit unit;

        protected String text;

        protected SentimentAnalysisListener listener;

        public SentimentUnitMeta(AIGCUnit unit, String text, SentimentAnalysisListener listener) {
            this.unit = unit;
            this.text = text;
            this.listener = listener;
        }

        public void process() {
            JSONObject data = new JSONObject();
            data.put("text", this.text);

            Packet request = new Packet(AIGCAction.Sentiment.name, data);
            ActionDialect dialect = cellet.transmit(this.unit.getContext(), request.toDialect(), 60 * 1000);
            if (null == dialect) {
                Logger.w(AIGCService.class, "Sentiment unit error");
                // 回调错误
                this.listener.onFailed();
                return;
            }

            Packet response = new Packet(dialect);
            JSONObject payload = Packet.extractDataPayload(response);

            SentimentResult result = new SentimentResult(payload);

            this.listener.onCompleted(result);
        }
    }

    private class SummarizationUnitMeta {

        protected AIGCUnit unit;

        protected String text;

        protected SummarizationListener listener;

        public SummarizationUnitMeta(AIGCUnit unit, String text, SummarizationListener listener) {
            this.unit = unit;
            this.text = text;
            this.listener = listener;
        }

        public void process() {
            JSONObject data = new JSONObject();
            data.put("text", this.text);

            Packet request = new Packet(AIGCAction.Summarization.name, data);
            ActionDialect dialect = cellet.transmit(this.unit.getContext(), request.toDialect(), 60 * 1000);
            if (null == dialect) {
                Logger.w(AIGCService.class, "Summarization unit error");
                // 回调错误
                this.listener.onFailed();
                return;
            }

            Packet response = new Packet(dialect);
            JSONObject payload = Packet.extractDataPayload(response);
            if (payload.has("summarization")) {
                this.listener.onCompleted(payload.getString("summarization"));
            }
            else {
                this.listener.onFailed();
            }
        }
    }

    private class TextToImageUnitMeta {

        protected long sn;

        protected AIGCUnit unit;

        protected AIGCChannel channel;

        protected String text;

        protected FileLabel fileLabel;

        protected TextToImageListener listener;

        public TextToImageUnitMeta(AIGCUnit unit, AIGCChannel channel, String text, TextToImageListener listener) {
            this.sn = Utils.generateSerialNumber();
            this.unit = unit;
            this.channel = channel;
            this.text = text;
            this.listener = listener;
        }

        public void process() {
            this.channel.setLastUnitMetaSn(this.sn);

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.onProcessing(channel);
                }
            });

            JSONObject data = new JSONObject();
            data.put("text", this.text);

            Packet request = new Packet(AIGCAction.TextToImage.name, data);
            // 使用较长的超时时间，以便等待上传数据
            ActionDialect dialect = cellet.transmit(this.unit.getContext(), request.toDialect(), 180 * 1000);
            if (null == dialect) {
                Logger.w(AIGCService.class, "TextToImage unit error");
                this.channel.setProcessing(false);
                // 回调错误
                this.listener.onFailed(this.channel);
                return;
            }

            Packet response = new Packet(dialect);
            JSONObject payload = Packet.extractDataPayload(response);
            if (payload.has("fileLabel")) {
                this.fileLabel = new FileLabel(payload.getJSONObject("fileLabel"));
                this.fileLabel.resetURLsToken(this.channel.getAuthToken().getCode());

                // 记录
                AIGCGenerationRecord record = this.channel.appendRecord(this.text, this.fileLabel);
                record.sn = this.sn;
                this.listener.onCompleted(record);

                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        long contactId = channel.getAuthToken().getContactId();
                        List<String> tokens = calcTokens(text);
                        long promptTokens = tokens.size();
                        long completionTokens = (long) Math.floor(fileLabel.getFileSize() / 10240.0);
                        storage.updateUsage(contactId, completionTokens, promptTokens);
                    }
                });
            }
            else {
                this.listener.onFailed(this.channel);
            }

            // 重置状态
            this.channel.setProcessing(false);
        }
    }


    private class ExtractKeywordsUnitMeta {

        protected AIGCUnit unit;

        protected String text;

        protected ExtractKeywordsListener listener;

        public ExtractKeywordsUnitMeta(AIGCUnit unit, String text, ExtractKeywordsListener listener) {
            this.unit = unit;
            this.text = text;
            this.listener = listener;
        }

        public void process() {
            JSONObject data = new JSONObject();
            data.put("text", this.text);
            Packet request = new Packet(AIGCAction.ExtractKeywords.name, data);
            ActionDialect dialect = cellet.transmit(this.unit.getContext(), request.toDialect(), 60 * 1000);
            if (null == dialect) {
                Logger.w(AIGCService.class, "Extract keywords unit error");
                // 回调错误
                this.listener.onFailed();
                return;
            }

            Packet response = new Packet(dialect);
            JSONObject payload = Packet.extractDataPayload(response);
            if (payload.has("words")) {
                JSONArray array = payload.getJSONArray("words");
                List<String> words = new ArrayList<>(array.length());
                for (int i = 0; i < array.length(); ++i) {
                    String word = array.getString(i).replaceAll("\n", "");
                    if (word.length() == 0) {
                        continue;
                    }
                    words.add(word);
                }
                this.listener.onCompleted(this.text, words);
            }
            else {
                this.listener.onFailed();
            }
        }
    }


    private class ASRUnitMeta {

        protected AIGCUnit unit;

        protected FileLabel source;

        protected FileLabel input;

        protected AutomaticSpeechRecognitionListener listener;

        public ASRUnitMeta(AIGCUnit unit, FileLabel source, FileLabel input,
                           AutomaticSpeechRecognitionListener listener) {
            this.unit = unit;
            this.source = source;
            this.input = input;
            this.listener = listener;
        }

        public void process() {
            JSONObject data = new JSONObject();
            data.put("input", this.input.toJSON());

            Packet request = new Packet(AIGCAction.AutomaticSpeechRecognition.name, data);
            ActionDialect dialect = cellet.transmit(this.unit.getContext(), request.toDialect());
            if (null == dialect) {
                Logger.w(AIGCService.class, "ASR unit error");
                // 回调错误
                this.listener.onFailed(this.source);
                return;
            }

            Packet response = new Packet(dialect);
            JSONObject payload = Packet.extractDataPayload(response);
            if (!payload.has("list")) {
                Logger.w(AIGCService.class, "ASR unit process failed");
                // 回调错误
                this.listener.onFailed(this.source);
                return;
            }

            this.listener.onCompleted(this.input, new ASRResult(source, payload));

            if (!this.input.getFileCode().equals(this.source.getFileCode())) {
                // 输入文件和源文件不一致，删除输入文件
                AbstractModule fileStorage = getKernel().getModule("FileStorage");
                if (null != fileStorage) {
                    JSONObject deleteFile = new JSONObject();
                    deleteFile.put("action", FileStorageAction.DeleteFile.name);
                    deleteFile.put("domain", this.input.getDomain().getName());
                    deleteFile.put("fileCode", this.input.getFileCode());
                    fileStorage.notify(deleteFile);
                }
            }
        }
    }


    protected class MutableArticleQuery {

        public PublicOpinion.ArticleQuery articleQuery;

        public MutableArticleQuery() {
        }
    }
}
