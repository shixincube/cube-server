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

package cube.service.aigc;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.*;
import cube.aigc.attachment.ui.Event;
import cube.aigc.attachment.ui.EventResult;
import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.PsychologyReport;
import cube.aigc.psychology.Theme;
import cube.aigc.publicopinion.PublicOpinionTaskName;
import cube.auth.AuthConsts;
import cube.auth.AuthToken;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.action.FileProcessorAction;
import cube.common.action.FileStorageAction;
import cube.common.entity.*;
import cube.common.notice.GetFile;
import cube.common.notice.LoadFile;
import cube.common.state.AIGCStateCode;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.file.FileProcessResult;
import cube.file.hook.FileStorageHook;
import cube.file.operation.AudioCropOperation;
import cube.file.operation.ExtractAudioOperation;
import cube.service.aigc.command.Command;
import cube.service.aigc.command.CommandListener;
import cube.service.aigc.knowledge.KnowledgeBase;
import cube.service.aigc.knowledge.KnowledgeFramework;
import cube.service.aigc.listener.*;
import cube.service.aigc.module.ModuleManager;
import cube.service.aigc.module.PublicOpinion;
import cube.service.aigc.module.Stage;
import cube.service.aigc.module.StageListener;
import cube.service.aigc.plugin.*;
import cube.service.aigc.resource.Agent;
import cube.service.aigc.resource.ResourceAnswer;
import cube.service.aigc.scene.PsychologyScene;
import cube.service.aigc.scene.PsychologySceneListener;
import cube.service.auth.AuthService;
import cube.service.auth.AuthServiceHook;
import cube.service.contact.ContactHook;
import cube.service.contact.ContactManager;
import cube.service.tokenizer.Tokenizer;
import cube.storage.StorageType;
import cube.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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
    private Map<String, Queue<GenerateTextUnitMeta>> generateQueueMap;

    /**
     * Key 是 AIGC 的 Query Key
     */
    private final Map<String, Queue<ConversationUnitMeta>> conversationQueueMap;

    private volatile ConversationUnitMeta currentConversationUnitMeta;

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

    private ConcurrentHashMap<String, AIGCChannel> channelMap;

    /**
     * 知识库架构。
     */
    private KnowledgeFramework knowledgeFramework;

    private long channelTimeout = 30 * 60 * 1000;

    private ExecutorService executor;

    private AIGCStorage storage;

    private Tokenizer tokenizer;

    private AIGCPluginSystem pluginSystem;

    /**
     * 是否对 Prompt 进行上下文识别。
     */
    private boolean enabledRecognizeContext = false;

    /**
     * 是否启用搜索关键词。
     */
    private boolean enabledSearch = false;

    /**
     * 是否访问，仅用于本地测试
     */
    private boolean useAgent = false;

    /**
     * 配置文件最后修改时间。
     */
    private long configFileLastModified = 0;
    // 配置文件上一次检测事件
    private long configFileLastTime = 0;

    public AIGCService(AIGCCellet cellet) {
        this.cellet = cellet;
        this.unitMap = new ConcurrentHashMap<>();
        this.channelMap = new ConcurrentHashMap<>();
        this.generateQueueMap = new ConcurrentHashMap<>();
        this.conversationQueueMap = new ConcurrentHashMap<>();
        this.nlTaskQueueMap = new ConcurrentHashMap<>();
        this.sentimentQueueMap = new ConcurrentHashMap<>();
        this.summarizationQueueMap = new ConcurrentHashMap<>();
        this.textToImageQueueMap = new ConcurrentHashMap<>();
        this.extractKeywordsQueueMap = new ConcurrentHashMap<>();
        this.asrQueueMap = new ConcurrentHashMap<>();
        this.tokenizer = new Tokenizer();
    }

    @Override
    public void start() {
        this.executor = Executors.newCachedThreadPool();

        this.pluginSystem = new AIGCPluginSystem();

        // 读取配置文件
        this.loadConfig();

        (new Thread(new Runnable() {
            @Override
            public void run() {
                // 启动心理学场景
                PsychologyScene.getInstance().start(AIGCService.this);

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

                // 知识库事件
                KnowledgeBaseEventPlugin plugin = new KnowledgeBaseEventPlugin(AIGCService.this);
                pluginSystem.register(AIGCHook.ImportKnowledgeDoc, plugin);
                pluginSystem.register(AIGCHook.RemoveKnowledgeDoc, plugin);

                // 监听授权服务事件
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

                // 监听联系人服务事件
                while (!ContactManager.getInstance().isStarted()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                ContactManager.getInstance().getPluginSystem().register(ContactHook.NewContact,
                        new ActivateKnowledgeBasePlugin(AIGCService.this));
                ContactEventPlugin contactPlugin = new ContactEventPlugin(AIGCService.this);
                ContactManager.getInstance().getPluginSystem().register(ContactHook.SignIn, contactPlugin);
                ContactManager.getInstance().getPluginSystem().register(ContactHook.SignOut, contactPlugin);
                ContactManager.getInstance().getPluginSystem().register(ContactHook.DeviceTimeout, contactPlugin);

                // 监听文件服务事件
                AbstractModule fileStorage = getKernel().getModule("FileStorage");
                if (null != fileStorage) {
                    while (!fileStorage.isStarted()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    fileStorage.getPluginSystem().register(FileStorageHook.SaveFile,
                            new NewFilePlugin(AIGCService.this));
                    fileStorage.getPluginSystem().register(FileStorageHook.DestroyFile,
                            new DeleteFilePlugin(AIGCService.this));
                }
                else {
                    Logger.e(AIGCService.class, "#start - Can NOT find \"FileStorage\" module!");
                }

                // 实例化知识框架
                knowledgeFramework = new KnowledgeFramework(AIGCService.this, authService, fileStorage);

                // 资源管理器
                Explorer.getInstance().setup(AIGCService.this, tokenizer);

                started.set(true);
                Logger.i(AIGCService.class, "AIGC service is ready");
            }
        })).start();
    }

    @Override
    public void stop() {
        PsychologyScene.getInstance().stop();

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
    public AIGCPluginSystem getPluginSystem() {
        return this.pluginSystem;
    }

    @Override
    public void onTick(Module module, Kernel kernel) {
        // 周期 60 秒
//        Logger.i(AIGCService.class, "#onTick");

        long now = System.currentTimeMillis();

        if (now - this.configFileLastTime > 5 * 60 * 1000) {
            this.configFileLastTime = now;
            this.loadConfig();
        }

        // 删除失效的 Unit
        Iterator<AIGCUnit> unitIter = this.unitMap.values().iterator();
        while (unitIter.hasNext()) {
            AIGCUnit unit = unitIter.next();
            if (null != unit.getContext() && !unit.getContext().isValid()) {
                // 已失效
                unitIter.remove();
            }
        }

        Iterator<AIGCChannel> iter = this.channelMap.values().iterator();
        while (iter.hasNext()) {
            AIGCChannel channel = iter.next();
            if (now - channel.getProcessingTimestamp() >= 3 * 60 * 1000) {
                // 重置状态
                channel.setProcessing(false);
            }

            if (now - channel.getActiveTimestamp() >= this.channelTimeout) {
                iter.remove();
            }
        }

        Explorer.getInstance().onTick(now);

        PsychologyScene.getInstance().onTick(now);
    }

    private void loadConfig() {
        try {
            File file = new File("config/aigc.properties");
            if (!file.exists()) {
                file = new File("aigc.properties");
            }

            if (this.configFileLastModified == file.lastModified()) {
//                if (Logger.isDebugLevel()) {
//                    Logger.d(this.getClass(), "#loadConfig - File not modified");
//                }
                return;
            }

            this.configFileLastModified = file.lastModified();

            Properties properties = ConfigUtils.readProperties(file.getAbsolutePath());
            this.enabledRecognizeContext = Boolean.parseBoolean(
                    properties.getProperty("enabled.recognize_context", "false"));
            this.enabledSearch = Boolean.parseBoolean(
                    properties.getProperty("enabled.search", "false"));

            // 上下文长度限制
            ModelConfig.EXTRA_LONG_CONTEXT_LIMIT = Integer.parseInt(
                    properties.getProperty("context.length",
                            Integer.toString(ModelConfig.EXTRA_LONG_CONTEXT_LIMIT)));
            ModelConfig.BAIZE_CONTEXT_LIMIT = Integer.parseInt(
                    properties.getProperty("context.length.baize",
                            Integer.toString(ModelConfig.BAIZE_CONTEXT_LIMIT)));
            ModelConfig.BAIZE_NEXT_CONTEXT_LIMIT = Integer.parseInt(
                    properties.getProperty("context.length.baize_next",
                            Integer.toString(ModelConfig.BAIZE_NEXT_CONTEXT_LIMIT)));

            // 页面阅读器 URL
            if (properties.containsKey("page.reader.url") || properties.containsKey("page.searcher")) {
                Explorer.getInstance().config(properties.getProperty("page.reader.url"),
                        properties.getProperty("page.searcher", "baidu"));
                Logger.i(this.getClass(), "AI Service - Page searcher: "
                        + Explorer.getInstance().getSearcherName());
                Logger.i(this.getClass(), "AI Service - Page reader url: "
                        + Explorer.getInstance().getPageReaderUrl());
            }

            // 是否启用代理
            this.useAgent = Boolean.parseBoolean(
                    properties.getProperty("agent", "false"));
            if (this.useAgent) {
                Agent.createInstance(properties.getProperty("agent.url", "http://127.0.0.1:7010"),
                        properties.getProperty("agent.token", ""));
                // 添加单元
                this.unitMap.put(Agent.getInstance().getUnit().getQueryKey(), Agent.getInstance().getUnit());
            }
        } catch (IOException e) {
            Logger.e(this.getClass(), "#loadConfig - Load config properties error", e);
        }

        Logger.i(this.getClass(), "AI Service - Recognize Context Enabled: " + this.enabledRecognizeContext);
        Logger.i(this.getClass(), "AI Service - Search Enabled: " + this.enabledSearch);
        Logger.i(this.getClass(), "AI Service - Context length: " + ModelConfig.EXTRA_LONG_CONTEXT_LIMIT);
        Logger.i(this.getClass(), "AI Service - Baize context limit: " + ModelConfig.BAIZE_CONTEXT_LIMIT);
        Logger.i(this.getClass(), "AI Service - BaizeNext context limit: " + ModelConfig.BAIZE_NEXT_CONTEXT_LIMIT);
        if (this.useAgent) {
            Logger.i(this.getClass(), "AI Service - Agent URL: " + Agent.getInstance().getUrl());
        }
    }

    public AIGCCellet getCellet() {
        return this.cellet;
    }

    public AIGCStorage getStorage() {
        return this.storage;
    }

    public Tokenizer getTokenizer() {
        return this.tokenizer;
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
                this.generateQueueMap.remove(unit.getQueryKey());
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

    public List<ModelConfig> getModelConfigs(JSONArray modelNames) {
        if (!this.isStarted()) {
            return null;
        }

        return this.storage.getModelConfigs(JSONUtils.toStringList(modelNames));
    }

    public List<Notification> getNotifications() {
        if (!this.isStarted()) {
            return null;
        }

        return this.storage.readEnabledNotifications();
    }

    public ContactPreference getPreference(long contactId) {
        return this.storage.readContactPreference(contactId);
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

        final String domain = AuthConsts.DEFAULT_DOMAIN;
        final String appKey = AuthConsts.DEFAULT_APP_KEY;

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

    public KnowledgeFramework getKnowledgeFramework() {
        return this.knowledgeFramework;
    }

    /**
     * 获取指定联系人的知识库信息列表。
     *
     * @param tokenCode
     * @return
     */
    public List<KnowledgeBaseInfo> getKnowledgeBaseInfoList(String tokenCode) {
        AuthToken authToken = this.getToken(tokenCode);
        if (null == authToken) {
            return null;
        }

        return this.knowledgeFramework.getKnowledgeBaseInfos(authToken.getContactId());
    }

    /**
     * 获取对应的知识库实例。
     *
     * @param tokenCode
     * @param baseName
     * @return
     */
    public KnowledgeBase getKnowledgeBase(String tokenCode, String baseName) {
        AuthToken authToken = this.getToken(tokenCode);
        if (null == authToken) {
            return null;
        }
        return this.getKnowledgeBase(authToken.getContactId(), baseName);
    }

    /**
     *
     * @param tokenCode
     * @param category
     * @return
     */
    public List<KnowledgeBase> getKnowledgeBaseByCategory(String tokenCode, String category) {
        AuthToken authToken = this.getToken(tokenCode);
        if (null == authToken) {
            return null;
        }
        return this.getKnowledgeBaseByCategory(authToken.getContactId(), category);
    }

    /**
     * 获取对应的知识库实例。
     *
     * @param contactId
     * @param baseName
     * @return
     */
    public synchronized KnowledgeBase getKnowledgeBase(Long contactId, String baseName) {
        return this.knowledgeFramework.getKnowledgeBase(contactId, baseName);
    }

    public synchronized List<KnowledgeBase> getKnowledgeBaseByCategory(Long contactId, String category) {
        return this.knowledgeFramework.getKnowledgeBaseByCategory(contactId, category);
    }

    /**
     * 对历史问答进行评价。
     *
     * @param token
     * @param historySN
     * @param feedback
     */
    public void evaluate(String token, long historySN, int feedback) {
        AIGCChannel channel = this.getChannelByToken(token);
        if (null != channel) {
            channel.feedbackRecord(historySN, feedback);
        }

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                storage.updateChatHistoryFeedback(historySN, feedback);
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
     * 停止频道正在进行的操作。
     *
     * @param channelCode 指定频道码。
     * @return
     */
    public AIGCChannel stopProcessing(String channelCode) {
        AIGCChannel channel = this.getChannel(channelCode);
        if (null == channel) {
            return null;
        }

        if (channel.isProcessing()) {
            boolean hit = false;

            // 进入队列，但是没有被执行线程处理
            for (Queue<GenerateTextUnitMeta> queue : this.generateQueueMap.values()) {
                Iterator<GenerateTextUnitMeta> iter = queue.iterator();
                while (iter.hasNext()) {
                    GenerateTextUnitMeta meta = iter.next();
                    if (meta.channel.getCode().equals(channelCode)) {
                        iter.remove();
                        channel.setProcessing(false);
                        hit = true;
                        break;
                    }
                }
                if (hit) {
                    break;
                }
            }

            if (!hit) {
                // 已经执行
                this.cellet.interrupt(channel.getLastUnitMetaSn());
            }
        }

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
                else {
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

                this.generateText(channel, unit, maq.articleQuery.query, maq.articleQuery.query,
                        new GenerativeOption(), null, null, false, false, new GenerateTextListener() {
                    @Override
                    public void onGenerated(AIGCChannel channel, GenerativeRecord record) {
                        maq.articleQuery.answer = record.answer.replaceAll(",", "，");
                        synchronized (result) {
                            result.append(maq.articleQuery.output());
                            result.notify();
                        }
                    }

                    @Override
                    public void onFailed(AIGCChannel channel, AIGCStateCode stateCode) {
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
     * 生成文本内容。
     *
     * @param channelCode
     * @param content
     * @param unitName
     * @param option
     * @param numHistories
     * @param records
     * @param categories
     * @param recordable
     * @param searchable
     * @param networking
     * @param listener
     * @return
     */
    public boolean generateText(String channelCode, String content, String unitName, GenerativeOption option,
                                int numHistories, List<GenerativeRecord> records, List<String> categories,
                                boolean recordable, boolean searchable, boolean networking,
                                GenerateTextListener listener) {
        if (!this.isStarted()) {
            Logger.w(AIGCService.class, "#generateText - Service is NOT ready");
            return false;
        }

        if (content.length() > ModelConfig.getPromptLengthLimit(unitName)) {
            Logger.w(AIGCService.class, "#generateText - Content length greater than "
                    + ModelConfig.getPromptLengthLimit(unitName));
            return false;
        }

        // 获取频道
        AIGCChannel channel = this.channelMap.get(channelCode);
        if (null == channel) {
            Logger.w(AIGCService.class, "#generateText - Can NOT find AIGC channel: " + channelCode);
            return false;
        }

        // 如果频道正在应答上一次问题，则返回 null
        if (channel.isProcessing()) {
            Logger.w(AIGCService.class, "#generateText - Channel is processing: " + channelCode);
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
            Logger.w(AIGCService.class, "#generateText - No conversational task unit setup in server");
            channel.setProcessing(false);
            return false;
        }

        GenerateTextUnitMeta meta = new GenerateTextUnitMeta(unit, channel, content, option, categories, records, listener);
        meta.numHistories = numHistories;
        meta.searchEnabled = searchable && this.enabledSearch;
        meta.recordHistoryEnabled = recordable;
        meta.networkingEnabled = networking;

        synchronized (this.generateQueueMap) {
            Queue<GenerateTextUnitMeta> queue = this.generateQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.generateQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            unit.setRunning(true);

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processGenerateTextQueue(meta.unit.getQueryKey());
                }
            });
        }

        return true;
    }

    /**
     * 同步方式生成文本。
     *
     * @param unitName
     * @param prompt
     * @param option
     * @param history
     * @param participantContact
     * @return
     */
    public String syncGenerateText(String unitName, String prompt, GenerativeOption option,
                                   List<GenerativeRecord> history, Contact participantContact) {
        if (this.useAgent) {
            Logger.d(this.getClass(), "#syncGenerateText - Agent - \"" + unitName + "\" - history:"
                    + ((null != history) ? history.size() : 0));
            return Agent.getInstance().generateText(null, unitName, prompt, option, history);
        }

        AIGCUnit unit = this.selectUnitByName(unitName);
        if (null == unit) {
            return null;
        }

        JSONArray historyArray = new JSONArray();
        if (null != history) {
            for (GenerativeRecord record : history) {
                historyArray.put(record.toJSON());
            }
        }

        Contact participant = (null == participantContact) ?
                new Contact(1000, AuthConsts.DEFAULT_DOMAIN) : participantContact;

        long sn = Utils.generateSerialNumber();

        JSONObject data = new JSONObject();
        data.put("unit", unit.getCapability().getName());
        data.put("content", prompt);
        data.put("participant", participant.toCompactJSON());
        data.put("history", historyArray);
        data.put("option", (null == option) ? (new GenerativeOption()).toJSON() : option.toJSON());

        Packet request = new Packet(AIGCAction.Chat.name, data);
        ActionDialect dialect = this.cellet.transmit(unit.getContext(), request.toDialect(),
                5 * 60 * 1000, sn);
        if (null == dialect) {
            Logger.w(AIGCService.class, "#syncGenerateText - transmit failed, sn:" + sn);
            // 记录故障
            unit.markFailure(AIGCStateCode.UnitError.code, System.currentTimeMillis(), participant.getId());
            return null;
        }

        Packet response = new Packet(dialect);
        JSONObject payload = Packet.extractDataPayload(response);

        String responseText = "";
        try {
            responseText = payload.getString("response");
        } catch (Exception e) {
            Logger.w(AIGCService.class, "#syncGenerateText - failed, sn:" + sn);
            return null;
        }

        // 过滤中文字符
        responseText = TextUtils.filterChinese(unit, responseText);
        return responseText;
    }

    /**
     * 同步方式生成文本。
     *
     * @param authToken
     * @param unitName
     * @param prompt
     * @param option
     * @return
     */
    public String syncGenerateText(AuthToken authToken, String unitName, String prompt, GenerativeOption option) {
        AIGCUnit unit = this.selectUnitByName(unitName);
        if (null == unit) {
            return null;
        }

        Contact participant = ContactManager.getInstance().getContact(authToken.getDomain(), authToken.getContactId());
        if (null == participant) {
            Logger.w(this.getClass(), "#syncGenerateText(AuthToken) - Can NOT find participant: " + authToken.getCode());
            return null;
        }

        return this.syncGenerateText(unitName, prompt, option, null, participant);
    }

    /**
     * 生成文本。
     *
     * @param channel
     * @param unit
     * @param query
     * @param prompt
     * @param option
     * @param records
     * @param categories
     * @param searchable
     * @param recordable
     * @param listener
     */
    public void generateText(AIGCChannel channel, AIGCUnit unit, String query, String prompt, GenerativeOption option,
                             List<GenerativeRecord> records, List<String> categories,
                             boolean searchable, boolean recordable, GenerateTextListener listener) {
        if (this.useAgent) {
            unit = Agent.getInstance().getUnit();
        }

        if (null == unit) {
            // 没有单元数据
            listener.onFailed(channel, AIGCStateCode.NotFound);
            return;
        }

        GenerateTextUnitMeta meta = new GenerateTextUnitMeta(unit, channel, prompt, option, categories, records, listener);
        meta.setOriginalQuery(query);
        meta.setSearchEnabled(searchable);
        meta.setRecordHistoryEnabled(recordable);
        meta.setNetworkingEnabled(false);

        synchronized (this.generateQueueMap) {
            Queue<GenerateTextUnitMeta> queue = this.generateQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.generateQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            unit.setRunning(true);

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processGenerateTextQueue(meta.unit.getQueryKey());
                }
            });
        }
    }

    /**
     * 互动会话。
     *
     * @param tokenCode
     * @param channelCode
     * @param content
     * @param parameter
     * @param listener
     * @return
     */
    public long executeConversation(String tokenCode, String channelCode, String content, AIGCConversationParameter parameter,
                             ConversationListener listener) {
        if (!this.isStarted()) {
            Logger.w(AIGCService.class, "#conversation - Service is NOT ready");
            return 0;
        }

        if (content.length() > ModelConfig.getPromptLengthLimit(ModelConfig.BAIZE_NEXT_UNIT)) {
            Logger.w(AIGCService.class, "#conversation - Content length greater than "
                    + ModelConfig.getPromptLengthLimit(ModelConfig.BAIZE_NEXT_UNIT));
            return 0;
        }

        // 获取频道
        AIGCChannel channel = this.channelMap.get(channelCode);
        if (null == channel) {
            Logger.d(AIGCService.class, "#conversation - Can NOT find channel, create new channel: " + channelCode);
            // 创建频道
            channel = this.createChannel(tokenCode, "User-" + channelCode, channelCode);
        }

        // 如果频道正在应答上一次问题，则返回 null
        if (channel.isProcessing()) {
            Logger.w(AIGCService.class, "#conversation - Channel is processing: " + channelCode);
            return 0;
        }

        channel.setProcessing(true);

        // 查找有该能力的单元
        AIGCUnit unit = this.selectUnitByName(ModelConfig.BAIZE_NEXT_UNIT);
        if (null == unit) {
            Logger.w(AIGCService.class, "#conversation - No conversational task unit setup in server");
            channel.setProcessing(false);
            return 0;
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

        return meta.sn;
    }

    public AIGCConversationResponse queryConversation(String channelCode, long sn) {
        // 获取频道
        AIGCChannel channel = this.channelMap.get(channelCode);
        if (null == channel) {
            Logger.w(AIGCService.class, "#queryConversation - Can NOT find channel: " + channelCode);
            return null;
        }

        GenerativeRecord record = channel.getRecord(sn);
        if (null == record) {
            ConversationUnitMeta unitMeta = null;

            if (null != this.currentConversationUnitMeta && this.currentConversationUnitMeta.sn == sn) {
                unitMeta = this.currentConversationUnitMeta;
            }
            else {
                synchronized (this.conversationQueueMap) {
                    for (Queue<ConversationUnitMeta> queue : this.conversationQueueMap.values()) {
                        for (ConversationUnitMeta meta : queue) {
                            if (meta.sn == sn) {
                                unitMeta = meta;
                                break;
                            }
                        }

                        if (null != unitMeta) {
                            break;
                        }
                    }
                }
            }

            if (null == unitMeta) {
                Logger.w(this.getClass(), "#queryConversation - Can NOT find conversation : " + sn);
                return null;
            }

            AIGCConversationResponse result = new AIGCConversationResponse(unitMeta.sn,
                    unitMeta.unit.getCapability().getName());
            result.processing = true;
            return result;
        }
        else {
            return new AIGCConversationResponse(record);
        }
    }

    public boolean converseBy(AIGCChannel channel, AIGCUnit unit, String prompt, ConversationListener listener) {
        // TODO
        return false;
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

        // 修正文本内容
        String modified = text.replaceAll("\n", "。");

        SummarizationUnitMeta meta = new SummarizationUnitMeta(unit, modified, listener);

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
     * 查询联系人的用量数据。
     *
     * @param contactId
     * @return
     */
    public List<Usage> queryContactUsages(long contactId) {
        if (!this.isStarted()) {
            return null;
        }

        List<Usage> result = new ArrayList<>();

        List<ModelConfig> models = this.getModelConfigs();
        for (ModelConfig modelConfig : models) {
            Usage usage = this.storage.readUsage(contactId, modelConfig.getModel());
            if (null == usage) {
                // 跳过没有记录的模型
                continue;
            }

            usage.name = modelConfig.getName();
            result.add(usage);
        }

        return result;
    }

    /**
     * 心理学绘画测验。
     *
     * @param token
     * @param attribute
     * @param fileCode
     * @param theme
     * @param paragraphInferrable
     * @param listener
     * @return
     */
    public PsychologyReport generatePsychologyReport(String token, Attribute attribute, String fileCode,
                                                     Theme theme, boolean paragraphInferrable,
                                                     PsychologySceneListener listener) {
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

        AIGCChannel channel = this.getChannelByToken(token);
        if (null == channel) {
            channel = this.createChannel(token, "Baize", Utils.randomString(16));
        }

        // 生成报告
        PsychologyReport report = PsychologyScene.getInstance().generateEvaluationReport(channel,
                attribute, fileLabel, theme, listener);

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
     * 图像内物体检测。
     *
     * @param channelCode
     * @param fileCodeList
     * @param listener
     * @return
     */
    public boolean objectDetection(String channelCode, List<String> fileCodeList, ObjectDetectionListener listener) {
        AIGCChannel channel = this.channelMap.get(channelCode);
        if (null == channel) {
            Logger.w(this.getClass(), "#objectDetection - No channel: " + channelCode);
            return false;
        }

        final List<FileLabel> fileList = new ArrayList<>();
        for (String fileCode : fileCodeList) {
            FileLabel fileLabel = this.getFile(channel.getAuthToken().getDomain(), fileCode);
            if (null == fileLabel) {
                Logger.d(this.getClass(), "#objectDetection - Can NOT find file: " + fileCode);
                continue;
            }

            fileList.add(fileLabel);
            if (fileList.size() >= 10) {
                break;
            }
        }

        if (fileList.isEmpty()) {
            Logger.w(this.getClass(), "#objectDetection - No files: " + channel.getAuthToken().getContactId());
            return false;
        }

        (new Thread(new Runnable() {
            @Override
            public void run() {
                AIGCUnit unit = selectUnitBySubtask(AICapability.ComputerVision.ObjectDetection);
                if (null == unit) {
                    Logger.w(AIGCService.class, "#objectDetection - No unit");
                    listener.onFailed(fileList, AIGCStateCode.UnitNoReady);
                    return;
                }

                JSONArray list = new JSONArray();
                for (FileLabel fileLabel : fileList) {
                    list.put(fileLabel.toJSON());
                }
                JSONObject payload = new JSONObject();
                payload.put("code", channelCode);
                payload.put("list", list);
                Packet request = new Packet(AIGCAction.ObjectDetection.name, payload);
                ActionDialect dialect = cellet.transmit(unit.getContext(), request.toDialect(), 3 * 60 * 1000);
                if (null == dialect) {
                    Logger.w(AIGCService.class, "#objectDetection - Unit error");
                    // 回调错误
                    listener.onFailed(fileList, AIGCStateCode.UnitError);
                    return;
                }

                Packet response = new Packet(dialect);
                JSONObject data = Packet.extractDataPayload(response);
                if (!data.has("result")) {
                    Logger.w(AIGCService.class, "#objectDetection - Unit process failed");
                    // 回调错误
                    listener.onFailed(fileList, AIGCStateCode.Failure);
                    return;
                }

                List<ObjectDetectionResult> resultList = new ArrayList<>();
                JSONArray result = data.getJSONArray("result");
                for (int i = 0; i < result.length(); ++i) {
                    ObjectDetectionResult odr = new ObjectDetectionResult(result.getJSONObject(i));
                    resultList.add(odr);
                }
                // 回调结束
                listener.onCompleted(fileList, resultList);
            }
        })).start();

        return true;
    }

    public FileLabel getFile(String domain, String fileCode) {
        AbstractModule fileStorage = this.getKernel().getModule("FileStorage");
        if (null == fileStorage) {
            Logger.e(this.getClass(), "#getFile - File storage service is not ready");
            return null;
        }

        GetFile getFile = new GetFile(domain, fileCode);
        JSONObject fileLabelJson = fileStorage.notify(getFile);
        if (null == fileLabelJson) {
            Logger.e(this.getClass(), "#getFile - Get file failed: " + fileCode);
            return null;
        }

        return new FileLabel(fileLabelJson);
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
                    resource.fixContent();
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
                    resource.fixContent();
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
                resource.fixContent();
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
                    resource.fixContent();
                    result.addResource(resource);
                }
                else {
                    result = new ComplexContext(ComplexContext.Type.Complex);
                    JSONObject resPayload = new JSONObject();
                    resPayload.put("payload", list.getJSONObject(0));
                    HyperlinkResource resource = new HyperlinkResource(resPayload);
                    resource.fixContent();
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

    private void processGenerateTextQueue(String queryKey) {
        Queue<GenerateTextUnitMeta> queue = this.generateQueueMap.get(queryKey);
        if (null == queue) {
            Logger.w(AIGCService.class, "#processGenerateTextQueue - Not found unit: " + queryKey);
            AIGCUnit unit = this.unitMap.get(queryKey);
            if (null != unit) {
                unit.setRunning(false);
            }
            return;
        }

        AIGCUnit unit = null;

        GenerateTextUnitMeta meta = queue.poll();
        while (null != meta) {
            if (null == unit) {
                unit = meta.unit;
            }

            // 执行处理
            try {
                meta.process();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#processGenerateTextQueue - meta process", e);
            }

            meta = queue.poll();
        }

        if (null == unit) {
            unit = this.unitMap.get(queryKey);
            if (null != unit) {
                unit.setRunning(false);
            }
            else {
                Logger.e(this.getClass(), "#processGenerateTextQueue - Unit is null: " + queryKey);
            }
        }
        else {
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
            // 设置当前值
            this.currentConversationUnitMeta = meta;

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

        this.currentConversationUnitMeta = null;
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


    private abstract class UnitMeta {

        public UnitMeta() {
        }

        protected List<String> readFileContent(List<FileLabel> fileLabels) {
            List<String> result = new ArrayList<>();

            AbstractModule fileStorage = getKernel().getModule("FileStorage");
            for (FileLabel fileLabel : fileLabels) {
                if (fileLabel.getFileType() == FileType.TEXT
                        || fileLabel.getFileType() == FileType.TXT
                        || fileLabel.getFileType() == FileType.MD
                        || fileLabel.getFileType() == FileType.LOG) {
                    String fullpath = fileStorage.notify(new LoadFile(fileLabel.getDomain().getName(), fileLabel.getFileCode()));
                    if (null == fullpath) {
                        Logger.w(this.getClass(), "#readFileContent - Load file error: " + fileLabel.getFileCode());
                        continue;
                    }

                    try {
                        List<String> lines = Files.readAllLines(Paths.get(fullpath));
                        for (String text : lines) {
                            if (text.trim().length() < 3) {
                                continue;
                            }
                            result.add(text);
                        }
                    } catch (Exception e) {
                        Logger.w(this.getClass(), "#readFileContent - Read file error: " + fullpath);
                    }
                }
                else {
                    Logger.w(this.getClass(), "#readFileContent - File type error: " + fileLabel.getFileType().getMimeType());
                }
            }

            return result;
        }

        public abstract void process();
    }


    private class GenerateTextUnitMeta extends UnitMeta {

        protected final long sn;

        protected AIGCUnit unit;

        protected AIGCChannel channel;

        protected Contact participant;

        protected String content;

        protected String originalQuery;

        protected GenerativeOption option;

        protected List<GenerativeRecord> records;

        protected List<String> categories;

        protected int numHistories;

        protected GenerateTextListener listener;

        protected AIGCChatHistory history;

        protected boolean searchEnabled = false;

        protected boolean recordHistoryEnabled = true;

        protected boolean networkingEnabled = false;

        public GenerateTextUnitMeta(AIGCUnit unit, AIGCChannel channel, String content, GenerativeOption option,
                                    List<String> categories, List<GenerativeRecord> records,
                                    GenerateTextListener listener) {
            this.sn = Utils.generateSerialNumber();
            this.unit = unit;
            this.channel = channel;
            this.participant = ContactManager.getInstance().getContact(channel.getAuthToken().getDomain(),
                    channel.getAuthToken().getContactId());
            this.content = content;
            this.option = option;
            this.categories = categories;
            this.records = records;
            this.numHistories = (null != records) ? records.size() : 0;
            this.listener = listener;

            this.history = new AIGCChatHistory(this.sn, unit.getCapability().getName(), this.participant.getDomain().getName());
            this.history.queryContactId = channel.getAuthToken().getContactId();
            this.history.queryTime = System.currentTimeMillis();
            this.history.queryContent = content;
        }

        public void setSearchEnabled(boolean value) {
            this.searchEnabled = value;
        }

        public void setNetworkingEnabled(boolean value) {
            this.networkingEnabled = value;
        }

        public void setRecordHistoryEnabled(boolean value) {
            this.recordHistoryEnabled = value;
        }

        public void setOriginalQuery(String originalQuery) {
            if (null == originalQuery) {
                return;
            }

            this.originalQuery = originalQuery;
            this.history.queryContent = originalQuery;
        }

        @Override
        public void process() {
            this.channel.setLastUnitMetaSn(this.sn);

            // 识别内容
            ComplexContext complexContext = enabledRecognizeContext ?
                    recognizeContext(this.content, this.channel.getAuthToken()) :
                    new ComplexContext(ComplexContext.Type.Simplex);

            // 设置是否启用了搜素
            complexContext.setSearchable(this.searchEnabled);
            // 设置是否进行联网分析
            complexContext.setNetworking(this.networkingEnabled);

            GenerativeRecord result = null;

            final StringBuilder realPrompt = new StringBuilder(this.content);

            if (complexContext.isSimplex()) {
                // 一般文本

                int maxHistories = 10;
                if (ModelConfig.isExtraLongPromptUnit(this.unit.getCapability().getName())) {
                    // 考虑到用量，限制在20轮
                    maxHistories = 20;
                }

                // 提示词长度限制
                int lengthLimit = ModelConfig.getPromptLengthLimit(this.unit.getCapability().getName());
                lengthLimit -= this.content.length();

                JSONObject data = new JSONObject();
                data.put("unit", this.unit.getCapability().getName());
                data.put("content", this.content);
                data.put("participant", this.participant.toCompactJSON());
                data.put("option", this.option.toJSON());

                boolean useQueryAttachment = false;
                if (null != this.records) {
                    for (GenerativeRecord record : this.records) {
                        if (record.hasQueryFile() || record.hasQueryAddition()) {
                            useQueryAttachment = true;
                            break;
                        }
                    }
                }

                if (useQueryAttachment) {
                    // 构建提示词
                    StringBuilder buf = new StringBuilder();
                    int bufLen = 0;

                    for (GenerativeRecord record : this.records) {
                        if (record.hasQueryAddition()) {
                            for (String text : record.queryAdditions) {
                                String[] qaBuf = text.split("\n");
                                for (String s : qaBuf) {
                                    if (s.trim().length() <= 1) {
                                        continue;
                                    }

                                    // 计算长度
                                    bufLen = buf.length() + s.length();
                                    if (bufLen >= lengthLimit) {
                                        break;
                                    }

                                    buf.append(s).append("\n");
                                }

                                if (bufLen >= lengthLimit) {
                                    break;
                                }
                            }

                            Logger.d(this.getClass(), "#process - Attachment text content length: "
                                    + buf.length());
                        }

                        if (bufLen >= lengthLimit) {
                            break;
                        }

                        if (record.hasQueryFile()) {
                            // 读取文件内容
                            List<String> fileContent = this.readFileContent(record.queryFileLabels);

                            if (!fileContent.isEmpty()) {
                                for (String text : fileContent) {
                                    // 计算长度
                                    bufLen = buf.length() + text.length();
                                    if (bufLen >= lengthLimit) {
                                        break;
                                    }

                                    buf.append(text).append("\n");
                                }

                                Logger.d(this.getClass(), "#process - Attachment file content length: "
                                        + buf.length());
                            }
                            else {
                                Logger.d(this.getClass(), "#process - Attachment file error: "
                                        + record.queryFileLabels.get(0).getFileName());
                            }
                        }

                        if (bufLen >= lengthLimit) {
                            break;
                        }
                    }

                    // 处理内容
                    try {
                        buf.delete(buf.length() - 1, buf.length());

                        realPrompt.delete(0, realPrompt.length());
                        realPrompt.append(Consts.formatQuestion(buf.toString(), this.content));

                        // 修改提示词
                        data.remove("content");
                        data.put("content", realPrompt.toString());

                        Logger.d(this.getClass(), "#process - Use query attachments creating the prompt - length: "
                                + realPrompt.length());
                    } catch (Exception e) {
                        Logger.w(this.getClass(), "#process", e);
                    }

                    // 无历史记录
                    data.put("history", new JSONArray());
                }
                else {
                    // 处理多轮历史记录
                    int lengthCount = 0;
                    List<GenerativeRecord> candidateRecords = new ArrayList<>();
                    if (null == this.records) {
                        int validNumHistories = Math.min(this.numHistories, maxHistories);
                        if (validNumHistories > 0) {
                            List<GenerativeRecord> records = this.channel.getLastHistory(validNumHistories);
                            // 正序列表转为倒序以便计算上下文长度
                            Collections.reverse(records);
                            for (GenerativeRecord record : records) {
                                // 判断长度
                                lengthCount += record.totalWords();
                                if (lengthCount > lengthLimit) {
                                    // 长度越界
                                    break;
                                }
                                // 加入候选
                                candidateRecords.add(record);
                            }
                            // 候选列表的倒序转为正序
                            Collections.reverse(candidateRecords);
                        }
                    }
                    else {
                        for (int i = this.records.size() - 1; i >= 0; --i) {
                            GenerativeRecord record = this.records.get(i);
                            lengthCount += record.totalWords();
                            // 判断长度
                            if (lengthCount > lengthLimit) {
                                // 长度越界
                                break;
                            }
                            // 加入候选
                            candidateRecords.add(record);
                            if (candidateRecords.size() >= maxHistories) {
                                break;
                            }
                        }
                        // 翻转顺序
                        Collections.reverse(candidateRecords);
                    }

                    // 加入分类释义
                    if (null != this.categories && !this.categories.isEmpty()) {
                        this.fillRecords(candidateRecords, this.categories, lengthLimit - lengthCount,
                                this.unit.getCapability().getName());
                    }

                    // 写入数组
                    JSONArray history = new JSONArray();
                    for (GenerativeRecord record : candidateRecords) {
                        history.put(record.toJSON());
                    }
                    data.put("history", history);
                }

                // 启用搜索或者启用联网信息检索都执行搜索
                if (this.searchEnabled || this.networkingEnabled) {
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            // 进行资源搜索
                            SearchResult searchResult = Explorer.getInstance().search(
                                    (null != originalQuery) ? originalQuery : content, channel.getAuthToken());
                            if (searchResult.hasResult() && networkingEnabled) {
                                // 执行搜索问答
                                performSearchPageQA(content, unit.getCapability().getName(),
                                        searchResult, complexContext, 3);
                            }
                            else {
                                // 没有搜索结果
                                complexContext.fixNetworkingResult(null, null);
                            }
                        }
                    });
                }

                if (Consts.NO_CONTENT_SENTENCE.equals(this.content)) {
                    // 知识库会使用 NO_CONTENT_SENTENCE 作为答案
                    String responseText = Consts.NO_CONTENT_SENTENCE + "。";
                    result = this.channel.appendRecord(this.sn, this.unit.getCapability().getName(),
                            (null != this.originalQuery) ? this.originalQuery : this.content,
                            responseText, complexContext);
                }
                else if (useAgent) {
                    String responseText = Agent.getInstance().generateText(channel.getCode(), this.content, this.records);
                    if (null != responseText) {
                        // 过滤中文字符
                        responseText = this.filterChinese(this.unit, responseText);
                        result = this.channel.appendRecord(this.sn, this.unit.getCapability().getName(),
                                (null != this.originalQuery) ? this.originalQuery : this.content,
                                responseText, complexContext);
                    }
                    else {
                        this.channel.setProcessing(false);
                        // 回调失败
                        this.listener.onFailed(this.channel, AIGCStateCode.UnitError);
                        return;
                    }
                }
                else {
                    Packet request = new Packet(AIGCAction.Chat.name, data);
                    ActionDialect dialect = cellet.transmit(this.unit.getContext(), request.toDialect(),
                            3 * 60 * 1000, this.sn);
                    if (null == dialect) {
                        Logger.w(AIGCService.class, "Unit error - channel: " + this.channel.getCode());
                        // 记录故障
                        this.unit.markFailure(AIGCStateCode.UnitError.code, System.currentTimeMillis(),
                                channel.getAuthToken().getContactId());
                        // 频道状态
                        this.channel.setProcessing(false);
                        // 回调错误
                        this.listener.onFailed(this.channel, AIGCStateCode.UnitError);
                        return;
                    }

                    // 是否被中断
                    if (cellet.isInterruption(dialect)) {
                        Logger.d(AIGCService.class, "Channel interrupted: " + this.channel.getCode());
                        this.channel.setProcessing(false);
                        // 回调错误
                        this.listener.onFailed(this.channel, AIGCStateCode.Interrupted);
                        return;
                    }

                    Packet response = new Packet(dialect);
                    JSONObject payload = Packet.extractDataPayload(response);

                    String responseText = "";
                    try {
                        responseText = payload.getString("response");
                    } catch (Exception e) {
                        Logger.w(AIGCService.class, "Unit respond failed - channel: " + this.channel.getCode());
                        // 记录故障
                        this.unit.markFailure(AIGCStateCode.Failure.code, System.currentTimeMillis(),
                                channel.getAuthToken().getContactId());
                        // 频道状态
                        this.channel.setProcessing(false);
                        // 回调错误
                        this.listener.onFailed(this.channel, AIGCStateCode.Failure);
                        return;
                    }

                    // 过滤中文字符
                    responseText = this.filterChinese(this.unit, responseText);
                    result = this.channel.appendRecord(this.sn, this.unit.getCapability().getName(),
                            (null != this.originalQuery) ? this.originalQuery : this.content,
                            responseText, complexContext);
                }
            }
            else {
                // 复合型数据
                ResourceAnswer resourceAnswer = new ResourceAnswer(complexContext);
                // 提取内容
                String content = resourceAnswer.extractContent(AIGCService.this, this.channel.getAuthToken());
                String answer = resourceAnswer.answer(content);
                result = this.channel.appendRecord(this.sn, this.unit.getCapability().getName(),
                        (null != this.originalQuery) ? this.originalQuery : this.content, answer, complexContext);
            }

            if (complexContext.isSimplex()) {
                if (this.searchEnabled || this.networkingEnabled) {
                    // 缓存上下文
                    Explorer.getInstance().cacheComplexContext(complexContext);
                }
            }
            else {
                // 缓存上下文
                Explorer.getInstance().cacheComplexContext(complexContext);
            }

            this.history.answerContactId = unit.getContact().getId();
            this.history.answerTime = System.currentTimeMillis();
            this.history.answerContent = result.answer;

            // 重置状态位
            this.channel.setProcessing(false);

            this.listener.onGenerated(this.channel, result);

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    // 更新用量
                    List<String> tokens = calcTokens(realPrompt.toString());
                    long promptTokens = tokens.size();
                    tokens = calcTokens(history.answerContent);
                    long completionTokens = tokens.size();
                    storage.updateUsage(history.queryContactId, ModelConfig.getModelByUnit(history.unit),
                            completionTokens, promptTokens);

                    // 保存历史记录
                    if (recordHistoryEnabled) {
                        storage.writeChatHistory(history);
                    }
                }
            });
        }

        protected String filterChinese(AIGCUnit unit, String text) {
            if (unit.getCapability().getName().equalsIgnoreCase(ModelConfig.CHAT_UNIT)) {
                if (TextUtils.containsChinese(text)) {
                    return text.replaceAll(",", "，");
                }
                else {
                    return text;
                }
            }
            else {
                return text;
            }
        }

        protected void performSearchPageQA(String query, String unitName, SearchResult searchResult,
                                         ComplexContext context, int maxPages) {
            Object mutex = new Object();
            AtomicInteger pageCount = new AtomicInteger(0);

            List<String> urlList = new ArrayList<>();
            for (SearchResult.OrganicResult or : searchResult.organicResults) {
                if (Explorer.getInstance().isIgnorableUrl(or.link)) {
                    // 跳过忽略的 URL
                    continue;
                }

                urlList.add(or.link);
                if (urlList.size() >= maxPages) {
                    break;
                }
            }

            List<Page> pages = new ArrayList<>();

            for (String url : urlList) {
                Explorer.getInstance().readPageContent(url, new ReadPageListener() {
                    @Override
                    public void onCompleted(String url, Page page) {
                        pageCount.incrementAndGet();

                        if (null != page) {
                            pages.add(page);
                        }

                        if (pageCount.get() >= urlList.size()) {
                            synchronized (mutex) {
                                mutex.notify();
                            }
                        }
                    }
                });
            }

            synchronized (mutex) {
                try {
                    mutex.wait(60 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            StringBuilder pageContent = new StringBuilder();

            for (Page page : pages) {
                StringBuilder buf = new StringBuilder();
                for (String text : page.textList) {
                    buf.append(text).append("\n");
                    if (buf.length() > ModelConfig.BAIZE_CONTEXT_LIMIT) {
                        break;
                    }
                }

                if (buf.length() > 2) {
                    buf.delete(buf.length() - 1, buf.length());
                    // 提取页面与提问匹配的信息
                    String prompt = Consts.formatExtractContent(buf.toString(), query);
                    String answer = syncGenerateText(this.channel.getAuthToken(), ModelConfig.CHAT_UNIT, prompt,
                            new GenerativeOption());
                    if (null != answer) {
                        // 记录内容
                        pageContent.append(answer);
                    }
                }
            }

            if (pageContent.length() <= 10) {
                Logger.d(this.getClass(), "#performSearchPageQA - No page content, cid:"
                        + this.channel.getAuthToken().getContactId());
                // 使用 null 值填充
                context.fixNetworkingResult(null, null);
                return;
            }

            final int lengthLimit = ModelConfig.getPromptLengthLimit(unitName);
            if (pageContent.length() > lengthLimit) {
                String[] tmp = pageContent.toString().split("。");
                pageContent = new StringBuilder();
                for (String text : tmp) {
                    pageContent.append(text).append("。");
                    if (pageContent.length() >= lengthLimit) {
                        break;
                    }
                }
                pageContent.delete(pageContent.length() - 1, pageContent.length());
            }

            // 对提取出来的内容进行推理
            String prompt = Consts.formatQuestion(pageContent.toString(), query);
            String result = syncGenerateText(this.channel.getAuthToken(), unitName, prompt, new GenerativeOption());
            if (null == result) {
                Logger.w(this.getClass(), "#performSearchPageQA - Infers page content failed, cid:"
                        + this.channel.getAuthToken().getContactId());
                // 使用 null 值填充
                context.fixNetworkingResult(null, null);
                return;
            }

            if (Logger.isDebugLevel()) {
                Logger.d(this.getClass(), "#performSearchPageQA - Result length: " + result.length());
            }

            // 将页面推理结果填充到上下文
            context.fixNetworkingResult(pages, result);
        }

        protected void fillRecords(List<GenerativeRecord> recordList, List<String> categories, int lengthLimit,
                                 String unitName) {
            int total = 0;
            for (String category : categories) {
                List<KnowledgeParaphrase> list = storage.readKnowledgeParaphrases(category);
                for (KnowledgeParaphrase paraphrase : list) {
                    total += paraphrase.getWord().length() + paraphrase.getParaphrase().length();
                    if (total > lengthLimit) {
                        break;
                    }

                    GenerativeRecord record = new GenerativeRecord(unitName,
                            paraphrase.getWord(), paraphrase.getParaphrase());
                    recordList.add(record);
                }

                if (total > lengthLimit) {
                    break;
                }
            }
        }
    }


    private class ConversationUnitMeta extends GenerateTextUnitMeta {

        protected AIGCConversationParameter parameter;

        protected ConversationListener conversationListener;

        protected GenerateTextListener generateListener = new GenerateTextListener() {
            @Override
            public void onGenerated(AIGCChannel channel, GenerativeRecord record) {
                AIGCConversationResponse response = new AIGCConversationResponse(record);
                conversationListener.onConversation(channel, response);
            }
            @Override
            public void onFailed(AIGCChannel channel, AIGCStateCode stateCode) {
                conversationListener.onFailed(channel, stateCode);
            }
        };

        public ConversationUnitMeta(AIGCUnit unit, AIGCChannel channel, String content,
                                    AIGCConversationParameter parameter,
                                    ConversationListener listener) {
            super(unit, channel, content, parameter.toGenerativeOption(), parameter.categories, parameter.records, null);
            this.listener = this.generateListener;
            this.numHistories = parameter.histories;
            this.searchEnabled = parameter.searchable;
            this.recordHistoryEnabled = parameter.recordable;
            this.networkingEnabled = parameter.networking;
            this.parameter = parameter;
            this.conversationListener = listener;
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
                this.listener.onFailed(this.text, AIGCStateCode.UnitError);
                return;
            }

            Packet response = new Packet(dialect);
            JSONObject payload = Packet.extractDataPayload(response);
            if (payload.has("summarization")) {
                this.listener.onCompleted(this.text, payload.getString("summarization"));
            }
            else {
                Logger.w(AIGCService.class, "Summarization unit return error");
                this.listener.onFailed(this.text, AIGCStateCode.NoData);
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

        protected AIGCChatHistory history;

        public TextToImageUnitMeta(AIGCUnit unit, AIGCChannel channel, String text, TextToImageListener listener) {
            this.sn = Utils.generateSerialNumber();
            this.unit = unit;
            this.channel = channel;
            this.text = text;
            this.listener = listener;

            this.history = new AIGCChatHistory(this.sn, unit.getCapability().getName(),
                    channel.getDomain().getName());
            this.history.queryContactId = channel.getAuthToken().getContactId();
            this.history.queryTime = System.currentTimeMillis();
            this.history.queryContent = text;
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
            ActionDialect dialect = cellet.transmit(this.unit.getContext(), request.toDialect(),
                    180 * 1000, this.sn);
            if (null == dialect) {
                Logger.w(AIGCService.class, "TextToImage unit error");
                this.channel.setProcessing(false);
                // 回调错误
                this.listener.onFailed(this.channel, AIGCStateCode.UnitError);
                return;
            }

            // 是否被中断
            if (cellet.isInterruption(dialect)) {
                Logger.d(AIGCService.class, "Channel interrupted: " + this.channel.getCode());
                this.channel.setProcessing(false);
                // 回调错误
                this.listener.onFailed(this.channel, AIGCStateCode.Interrupted);
                return;
            }

            Packet response = new Packet(dialect);
            JSONObject payload = Packet.extractDataPayload(response);
            if (payload.has("fileLabel")) {
                this.fileLabel = new FileLabel(payload.getJSONObject("fileLabel"));
                this.fileLabel.resetURLsToken(this.channel.getAuthToken().getCode());

                // 记录
                GenerativeRecord record = this.channel.appendRecord(this.sn, this.unit.getCapability().getName(),
                        this.text, this.fileLabel);
                this.listener.onCompleted(record);

                // 填写历史
                this.history.answerContactId = this.unit.getContact().getId();
                this.history.answerTime = System.currentTimeMillis();
                this.history.answerContent = this.fileLabel.toCompactJSON().toString();

                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        // 计算用量
                        long contactId = channel.getAuthToken().getContactId();
                        List<String> tokens = calcTokens(text);
                        long promptTokens = tokens.size();
                        long completionTokens = (long) Math.floor(fileLabel.getFileSize() / 1024.0);
                        storage.updateUsage(contactId, ModelConfig.getModelByUnit(history.unit),
                                completionTokens, promptTokens);

                        // 保存历史记录
                        storage.writeChatHistory(history);
                    }
                });
            }
            else {
                this.listener.onFailed(this.channel, AIGCStateCode.UnitError);
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
                this.listener.onFailed(this.text, AIGCStateCode.UnitError);
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

                if (words.isEmpty()) {
                    this.listener.onFailed(this.text, AIGCStateCode.NoData);
                }
                else {
                    this.listener.onCompleted(this.text, words);
                }
            }
            else {
                this.listener.onFailed(this.text, AIGCStateCode.DataStructureError);
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
