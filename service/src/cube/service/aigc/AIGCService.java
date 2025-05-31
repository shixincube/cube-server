/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.*;
import cube.aigc.ModelConfig;
import cube.aigc.app.Notification;
import cube.aigc.complex.widget.Event;
import cube.aigc.complex.widget.EventResult;
import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.ScaleReport;
import cube.aigc.psychology.Theme;
import cube.aigc.psychology.app.AppHelper;
import cube.aigc.psychology.composition.Scale;
import cube.auth.AuthConsts;
import cube.auth.AuthToken;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.action.FileProcessorAction;
import cube.common.entity.*;
import cube.common.notice.GetFile;
import cube.common.notice.LoadFile;
import cube.common.notice.SaveFile;
import cube.common.state.AIGCStateCode;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.file.hook.FileStorageHook;
import cube.service.aigc.command.Command;
import cube.service.aigc.command.CommandListener;
import cube.service.aigc.guidance.GuideFlow;
import cube.service.aigc.guidance.Guides;
import cube.service.aigc.knowledge.KnowledgeBase;
import cube.service.aigc.knowledge.KnowledgeFramework;
import cube.service.aigc.listener.*;
import cube.service.aigc.plugin.*;
import cube.service.aigc.resource.Agent;
import cube.service.aigc.resource.ResourceAnswer;
import cube.service.aigc.scene.PaintingReportListener;
import cube.service.aigc.scene.PsychologyScene;
import cube.service.aigc.scene.ScaleReportListener;
import cube.service.auth.AuthService;
import cube.service.auth.AuthServiceHook;
import cube.service.contact.ContactHook;
import cube.service.contact.ContactManager;
import cube.service.contact.ContactMask;
import cube.service.tokenizer.Tokenizer;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;
import cube.storage.StorageType;
import cube.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
public class AIGCService extends AbstractModule implements Generatable {

    public final static String NAME = "AIGC";

    private final AIGCCellet cellet;

    /**
     * Key 是 AIGC 的 Query Key
     */
    private final Map<String, AIGCUnit> unitMap;

    /**
     * 单元权重。
     * Key 是 Contact Id
     */
    private final Map<Long, Double> unitWeightMap;

    /**
     * Key 是 AIGC 的 Query Key
     */
    private final Map<String, Queue<GenerateTextUnitMeta>> generateQueueMap;

    /**
     * Key 是 AIGC 的 Query Key
     */
    private final Map<String, Queue<ConversationUnitMeta>> conversationQueueMap;

    /**
     * Key 是 AIGC 的 Query Key
     */
    private final Map<String, Queue<UnitMeta>> textToFileQueueMap;

    /**
     * Key 是 AIGC 的 Query Key
     */
    private final Map<String, Queue<UnitMeta>> textToImageQueueMap;

    /**
     * Key 是 AIGC 的 Query Key
     */
    private final Map<String, Queue<UnitMeta>> summarizationQueueMap;

    /**
     * Key 是 AIGC 的 Query Key
     */
    private final Map<String, Queue<UnitMeta>> extractKeywordsQueueMap;

    /**
     * Key 是 AIGC 的 Query Key
     */
    private final Map<String, Queue<UnitMeta>> semanticSearchQueueMap;

    /**
     * Key 是 AIGC 的 Query Key
     */
    private final Map<String, Queue<UnitMeta>> retrieveReRankQueueMap;

    /**
     * Key 是 AIGC 的 Query Key
     */
    private final Map<String, Queue<UnitMeta>> speechQueueMap;

    /**
     * 最大频道数量。
     */
    private final int maxChannel = 10000;

    private ConcurrentHashMap<String, AIGCChannel> channelMap;

    /**
     * 知识库架构。
     */
    private KnowledgeFramework knowledgeFramework;

    private final long channelTimeout = 30 * 60 * 1000;

    private ExecutorService executor;

    private AIGCStorage storage;

    private Tokenizer tokenizer;

    private AIGCPluginSystem pluginSystem;

    /**
     * 工作路径。
     */
    private final File workingPath = new File("storage/tmp/");

    /**
     * 生成文本任务执行实时计数。
     */
    private ConcurrentHashMap<String, AtomicInteger> generateTextUnitCountMap;

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
        this.unitWeightMap = new ConcurrentHashMap<>();
        this.channelMap = new ConcurrentHashMap<>();
        this.generateQueueMap = new ConcurrentHashMap<>();
        this.conversationQueueMap = new ConcurrentHashMap<>();
        this.textToFileQueueMap = new ConcurrentHashMap<>();
        this.textToImageQueueMap = new ConcurrentHashMap<>();
        this.summarizationQueueMap = new ConcurrentHashMap<>();
        this.extractKeywordsQueueMap = new ConcurrentHashMap<>();
        this.semanticSearchQueueMap = new ConcurrentHashMap<>();
        this.retrieveReRankQueueMap = new ConcurrentHashMap<>();
        this.speechQueueMap = new ConcurrentHashMap<>();
        this.generateTextUnitCountMap = new ConcurrentHashMap<>();
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
                if (!workingPath.exists()) {
                    if (!workingPath.mkdirs()) {
                        Logger.w(AIGCService.class, "AI Service - Make working path error: "
                                + workingPath.getAbsolutePath());
                    }
                }
                Logger.i(AIGCService.class, "AI Service - Working path: " + workingPath.getAbsolutePath());

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
                ContactManager.getInstance().getPluginSystem().register(ContactHook.VerifyVerificationCode, contactPlugin);

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

                // 引导系统列表
                List<GuideFlow> guideFlows = Guides.listGuideFlows();
                for (GuideFlow flow : guideFlows) {
                    Logger.i(AIGCService.class, "Guide flow: " + flow.getName());
                }

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
            if (now - channel.getProcessingTimestamp() >= 5 * 60 * 1000) {
                // 重置状态
                channel.setProcessing(false);
            }

            if (now - channel.getActiveTimestamp() >= this.channelTimeout) {
                iter.remove();
            }
        }

        if (null != this.knowledgeFramework) {
            this.knowledgeFramework.onTick(now);
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
            // 上下文长度限制
            ModelConfig.EXTRA_LONG_CONTEXT_LIMIT = Math.max(Integer.parseInt(
                        properties.getProperty("context.length",
                                Integer.toString(ModelConfig.EXTRA_LONG_CONTEXT_LIMIT))),
                    ModelConfig.EXTRA_LONG_CONTEXT_LIMIT);
            ModelConfig.BAIZE_CONTEXT_LIMIT = Math.max(Integer.parseInt(
                        properties.getProperty("context.length.baize",
                                Integer.toString(ModelConfig.BAIZE_CONTEXT_LIMIT))),
                    ModelConfig.BAIZE_CONTEXT_LIMIT);
            ModelConfig.BAIZE_X_CONTEXT_LIMIT = Math.max(Integer.parseInt(
                        properties.getProperty("context.length.baize_x",
                                Integer.toString(ModelConfig.BAIZE_X_CONTEXT_LIMIT))),
                    ModelConfig.BAIZE_X_CONTEXT_LIMIT);
            ModelConfig.BAIZE_NEXT_CONTEXT_LIMIT = Math.max(Integer.parseInt(
                        properties.getProperty("context.length.baize_next",
                                Integer.toString(ModelConfig.BAIZE_NEXT_CONTEXT_LIMIT))),
                    ModelConfig.BAIZE_NEXT_CONTEXT_LIMIT);

            // Unit 权重
            Iterator<Object> keyIter = properties.keySet().iterator();
            while (keyIter.hasNext()) {
                String key = keyIter.next().toString();
                if (key.startsWith("unit.weight.")) {
                    String[] seg = key.split("\\.");
                    if (seg.length == 3) {
                        try {
                            long cid = Long.parseLong(seg[2]);
                            double weight = Double.parseDouble(properties.getProperty(key, "1.0"));
                            unitWeightMap.put(cid, weight);
                            Logger.i(this.getClass(), "AI Service - Unit weight: " + cid + " - " + weight);
                        } catch (Exception e) {
                            // Nothing
                        }
                    }
                }
            }

            // 网络搜索配置
            if (properties.containsKey("page.reader.url") || properties.containsKey("page.searcher")) {
                Explorer.getInstance().config(properties.getProperty("page.searcher", "baidu"));
                Logger.i(this.getClass(), "AI Service - Page searcher: "
                        + Explorer.getInstance().getSearcherName());
            }

            // 是否启用代理
            this.useAgent = Boolean.parseBoolean(
                    properties.getProperty("agent", "false"));
            if (this.useAgent) {
                Agent.createInstance(properties.getProperty("agent.url", "http://127.0.0.1:7010"),
                        properties.getProperty("agent.token", ""));
                // 添加单元
                Agent.getInstance().fillUnits(this.unitMap);
            }
        } catch (IOException e) {
            Logger.e(this.getClass(), "#loadConfig - Load config properties error", e);
        }

        Logger.i(this.getClass(), "AI Service - Context length: " + ModelConfig.EXTRA_LONG_CONTEXT_LIMIT);
        Logger.i(this.getClass(), "AI Service - Baize context limit: " + ModelConfig.BAIZE_CONTEXT_LIMIT);
        Logger.i(this.getClass(), "AI Service - BaizeX context limit: " + ModelConfig.BAIZE_X_CONTEXT_LIMIT);
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

    public ExecutorService getExecutor() {
        return this.executor;
    }

    public File getWorkingPath() {
        return this.workingPath;
    }

    public List<AIGCUnit> setupUnit(Contact contact, List<AICapability> capabilities, TalkContext context) {
        List<AIGCUnit> result = new ArrayList<>(capabilities.size());

        for (AICapability capability : capabilities) {
            String key = AIGCUnit.makeQueryKey(contact, capability);
            AIGCUnit unit = this.unitMap.get(key);
            if (null != unit) {
                unit.setContext(context);
            }
            else {
                unit = new AIGCUnit(contact, capability, context);
                this.unitMap.put(key, unit);
            }

            Double weight = this.unitWeightMap.get(contact.getId());
            if (null != weight) {
                unit.setWeight(weight.doubleValue());
                Logger.d(this.getClass(), "#setupUnit - Modify unit \"" +
                        unit.getCapability().getName() + "\" weight : " + weight);
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
        List<AIGCChannel> list = new ArrayList<>(this.channelMap.values());
        list.addAll(this.channelMap.values());
        return list;
    }

    public int numUnitsByName(String unitName) {
        int num = 0;
        Iterator<AIGCUnit> iter = this.unitMap.values().iterator();
        while (iter.hasNext()) {
            AIGCUnit unit = iter.next();
            if (unit.getCapability().getName().equals(unitName)) {
                ++num;
            }
        }
        return num;
    }

    public boolean hasUnit(String unitName) {
        Iterator<AIGCUnit> iter = this.unitMap.values().iterator();
        while (iter.hasNext()) {
            if (iter.next().getCapability().getName().equals(unitName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 选择空闲的单元，如果没有空闲单元返回 <code>null</code> 值。
     *
     * @param unitName
     * @return
     */
    public AIGCUnit selectIdleUnitByName(String unitName) {
        ArrayList<AIGCUnit> candidates = new ArrayList<>();

        Iterator<AIGCUnit> iter = this.unitMap.values().iterator();
        while (iter.hasNext()) {
            AIGCUnit unit = iter.next();
            if (unit.getCapability().getName().equals(unitName)
                    && unit.getContext().isValid()
                    && !unit.isRunning()) {
                candidates.add(unit);
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        // 按照权重从高到低排序
        Collections.sort(candidates, new Comparator<AIGCUnit>() {
            @Override
            public int compare(AIGCUnit u1, AIGCUnit u2) {
                return (int)(u2.getWeight() - u1.getWeight());
            }
        });

        return candidates.get(0);
    }

    public AIGCUnit selectUnitByName(String unitName) {
        AIGCUnit idleUnit = this.selectIdleUnitByName(unitName);
        if (null != idleUnit) {
            return idleUnit;
        }

        ArrayList<AIGCUnit> candidates = new ArrayList<>();

        // 1. 选择所有无故障节点
        Iterator<AIGCUnit> iter = this.unitMap.values().iterator();
        while (iter.hasNext()) {
            AIGCUnit unit = iter.next();
            if (unit.getCapability().getName().equals(unitName) &&
                    unit.getContext().isValid() &&
                    unit.numFailure() == 0) {
                candidates.add(unit);
            }
        }

        if (candidates.isEmpty()) {
            // 2. 没有无故障节点，选择所有节点
            iter = this.unitMap.values().iterator();
            while (iter.hasNext()) {
                AIGCUnit unit = iter.next();
                if (unit.getCapability().getName().equals(unitName) &&
                        unit.getContext().isValid()) {
                    candidates.add(unit);
                }
            }
        }

        // 无候选节点
        if (candidates.isEmpty()) {
            return null;
        }

        int num = candidates.size();
        if (num == 1) {
            return candidates.get(0);
        }

        // 按照发生错误的数量从低到高排序
        Collections.sort(candidates, new Comparator<AIGCUnit>() {
            @Override
            public int compare(AIGCUnit u1, AIGCUnit u2) {
                return u1.numFailure() - u2.numFailure();
            }
        });

        // 按照权重从高到低排序
        Collections.sort(candidates, new Comparator<AIGCUnit>() {
            @Override
            public int compare(AIGCUnit u1, AIGCUnit u2) {
                return (int)(u2.getWeight() - u1.getWeight());
            }
        });

        // 先进行一次选择
        AIGCUnit unit = candidates.get(0);

        iter = candidates.iterator();
        while (iter.hasNext()) {
            AIGCUnit u = iter.next();
            if (u.isRunning()) {
                // 把正在运行的单元从候选列表里删除
                iter.remove();
            }
        }

        if (candidates.isEmpty()) {
            // 所有单元都在运行，返回选择
            return unit;
        }

        // 权重最高，且没有正在运行的节点
        unit = candidates.get(0);
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

    //-------- App Interface - Start --------

    public User getUser(String token) {
        User user = null;
        AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
        AuthToken authToken = authService.getToken(token);
        if (null == authToken) {
            Logger.w(this.getClass(), "#getUser - Can NOT find token: " + token);
            return null;
        }
        Contact contact = ContactManager.getInstance().getContact(token);
        if (null == contact) {
            Logger.w(this.getClass(), "#getUser - Can NOT find contact: " + token);
            return null;
        }
        user = new User(contact.getContext());
        user.setAuthToken(authToken);
        return user;
    }

    /**
     * 创建新用户。
     *
     * @param appAgent
     * @param device
     * @param channel
     * @return
     */
    public User createUser(String appAgent, Device device, String channel) {
        final String domain = AuthConsts.DEFAULT_DOMAIN;
        final String appKey = AuthConsts.DEFAULT_APP_KEY;
        final long tokenDuration = 5L * 365 * 24 * 60 * 60 * 1000;

        User user = null;

//        long id = Cryptology.getInstance().fastHash(appAgent);
//        try {
//            MessageDigest md5 = MessageDigest.getInstance("MD5");
//            byte[] digest = md5.digest(appAgent.getBytes(StandardCharsets.UTF_8));
//            long hash = Cryptology.getInstance().fastHash(digest);
//            id += hash;
//        } catch (Exception e) {
//            Logger.e(this.getClass(), "#getOrCreateUser", e);
//        }
//        // 处理 ID
//        id = Math.abs(id);

        long id = Long.parseLong(Utils.randomInt(10000, 99999) + Utils.randomNumberString(5));
        while (ContactManager.getInstance().containsContact(domain, id)) {
            Logger.e(this.getClass(), "#getOrCreateUser - Retry contact id: " + id);
            id = Long.parseLong(Utils.randomInt(10000, 99999) + Utils.randomNumberString(5));
        }

        // 创建令牌
        AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
        // 5年有效时长
        AuthToken authToken = authService.applyToken(domain, appKey, id, tokenDuration);

        String name = "ME" + Utils.randomNumberString(8);
        user = new User(id, name, appAgent, channel);
        user.setDisplayName("未登录");
        user.setAuthToken(authToken);

        // 新用户
        Contact contact = ContactManager.getInstance().newContact(id, domain, name, user.toJSON(), device);

        // 初始积分
        ContactManager.getInstance().getPointSystem().insert(AppHelper.createNewUserPoint(contact, 1000));

        return user;
    }

    public User updateUser(Contact contact, VerificationCode verificationCode) {
        // 查找用户
        ContactSearchResult searchResult = ContactManager.getInstance().searchWithContactName(
                contact.getDomain().getName(), verificationCode.phoneNumber);
        if (searchResult.getContactList().isEmpty()) {
            Logger.i(this.getClass(), "#updateUser - New user: " + contact.getId());

            // 新注册用户
            User user = new User(contact.getContext());
            user.setDisplayName(verificationCode.phoneNumber);
            user.setPhoneNumber(verificationCode.dialCode + "-" + verificationCode.phoneNumber);

            ContactManager.getInstance().updateContact(contact.getDomain().getName(),
                    contact.getId(), verificationCode.phoneNumber, user.toJSON());
            return user;
        }
        else {
            Logger.i(this.getClass(), "#updateUser - User login: " + contact.getId());

            // 老用户登录
            // 老用户当前使用的令牌删除，但是不删除设备的临时联系人
            Contact userContact = searchResult.getContactList().get(0);
            AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
            // 当前使用令牌码
            AuthToken currentToken = authService.getToken(contact.getDomain().getName(), contact.getId());
            String tokenCode = null;
            if (null != currentToken) {
                tokenCode = currentToken.getCode();
                // 删除当前临时联系人的令牌
                authService.deleteToken(contact.getDomain().getName(), contact.getId());
            }
            else {
                tokenCode = Utils.randomString(32);
            }

            // 临时联系人标记为作废
            if (contact.getId().longValue() != userContact.getId().longValue()) {
                ContactManager.getInstance().setContactMask(contact.getDomain().getName(), contact.getId(),
                        ContactMask.Deprecated);
            }

            // 更新老用户的令牌
            final long tokenDuration = 5L * 365 * 24 * 60 * 60 * 1000;
            AuthToken authToken = new AuthToken(tokenCode, userContact.getDomain().getName(),
                    AuthConsts.DEFAULT_APP_KEY, userContact.getId(),
                    System.currentTimeMillis(), System.currentTimeMillis() + tokenDuration, false);
            AuthToken newToken = authService.updateAuthTokenCode(authToken);
            User user = new User(userContact.getContext());
            user.setAuthToken(newToken);

            ContactManager.getInstance().updateContact(userContact.getDomain().getName(),
                    userContact.getId(), verificationCode.phoneNumber, user.toJSON());
            return user;
        }
    }

    /**
     * 注销用户。
     *
     * @param contact
     * @return
     */
    public User signOutUser(Contact contact) {
        Logger.i(this.getClass(), "#signOutUser - User: " + contact.getId());

        ContactManager.getInstance().setContactMask(contact.getDomain().getName(), contact.getId(),
                ContactMask.SignOut);

        User user = new User(contact.getContext());
        user.setAuthToken(null);
        // 更新名称
        ContactManager.getInstance().updateContact(contact.getDomain().getName(), contact.getId(),
                contact.getName() + "-" + ContactMask.SignOut.mask, user.toJSON());
        // 删除令牌
        AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
        authService.deleteToken(contact.getDomain().getName(), contact.getId());
        return user;
    }

    public WordCloud createWordCloud(AuthToken authToken) {
        WordCloud wordCloud = new WordCloud();

        long end = System.currentTimeMillis();
        long start = end - (365L * 24 * 60 * 60 * 1000);
        List<AIGCChatHistory> chatHistories = this.storage.readHistoriesByContactId(
                authToken.getContactId(), authToken.getDomain(), start, end);

        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(this.tokenizer);
        for (AIGCChatHistory history : chatHistories) {
            List<String> words = analyzer.analyzeOnlyWords(history.queryContent, 5);
            for (String word : words) {
                wordCloud.addWord(word.trim());
            }
            words = analyzer.analyzeOnlyWords(history.answerContent, 5);
            for (String word : words) {
                wordCloud.addWord(word.trim());
            }
        }

        return wordCloud;
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
            return new ArrayList<>();
        }

        try {
            return this.storage.readEnabledNotifications();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public ContactPreference getPreference(long contactId) {
        return this.storage.readContactPreference(contactId);
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

        ContactSearchResult searchResult = ContactManager.getInstance().searchWithContactId(domain, phone);
        if (searchResult.getContactList().isEmpty()) {
            // 没有该联系人
            Contact contact = ContactManager.getInstance().newContact(phone,
                    domain, (null != userName) ? userName : phoneNumber, null, null);
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

    //-------- App Interface - End --------

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
                storage.updateHistoryFeedback(historySN, feedback);
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

    /*public String inferByModule(String token, String moduleName, JSONObject params) {
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
            OpinionTaskName task = OpinionTaskName.parse(taskName);
            if (null == task) {
                Logger.d(this.getClass(), "#inferByModule - PublicOpinion task is unknown: " + taskName);
                return null;
            }

            if (task == OpinionTaskName.ArticleSentimentSummary ||
                task == OpinionTaskName.ArticleSentimentClassification) {
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
                if (OpinionTaskName.ArticleSentimentSummary == task) {
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
                        new GeneratingOption(), null, 0, null,
                        null, false, false, new GenerateTextListener() {
                    @Override
                    public void onGenerated(AIGCChannel channel, GeneratingRecord record) {
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
    }*/

    /**
     * 生成文本内容。
     *
     * @param channelCode 频道代码。
     * @param content 内容。
     * @param unitName 单元名。
     * @param option 生成参数设置。
     * @param histories 历史记录别表。
     * @param maxHistories 最大历史记录数量。
     * @param attachments 附件信息。
     * @param categories 分类信息。
     * @param recordable 是否记录到库。
     * @param networking 是否进行联网操作。
     * @param listener 监听器。
     * @return
     */
    public boolean generateText(String channelCode, String content, String unitName, GeneratingOption option,
                                List<GeneratingRecord> histories, int maxHistories, List<GeneratingRecord> attachments,
                                List<String> categories, boolean recordable, boolean networking,
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
            unit = Agent.getInstance().selectUnit(unitName);
        }
        else {
            unit = this.selectUnitByName(unitName);
            if (null == unit) {
                unit = this.selectUnitBySubtask(AICapability.NaturalLanguageProcessing.Conversational);
            }
        }

        if (null == unit) {
            Logger.w(AIGCService.class, "#generateText - No conversational task unit setup in server");
            channel.setProcessing(false);
            return false;
        }

        GenerateTextUnitMeta meta = new GenerateTextUnitMeta(unit, channel, content, option, categories,
                histories, attachments, listener);
        meta.maxHistories = maxHistories;
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
     * 返回生成文本单元实时运行计数。
     *
     * @return
     */
    public Map<String, AtomicInteger> getGenerateTextUnitRealtimeCount() {
        return this.generateTextUnitCountMap;
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
    public GeneratingRecord syncGenerateText(AuthToken authToken, String unitName, String prompt, GeneratingOption option) {
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
     * 同步方式生成文本。
     *
     * @param unitName
     * @param prompt
     * @param option
     * @param history
     * @param participantContact
     * @return
     */
    public GeneratingRecord syncGenerateText(String unitName, String prompt, GeneratingOption option,
                                             List<GeneratingRecord> history, Contact participantContact) {
        AIGCUnit unit = this.selectIdleUnitByName(unitName);
        if (null == unit) {
            unit = this.selectUnitByName(unitName);
            if (null == unit) {
                Logger.w(this.getClass(), "#syncGenerateText - Can NOT find unit: " + unitName);
                return null;
            }
        }
        return this.syncGenerateText(unit, prompt, option, history, participantContact);
    }

    /**
     * 同步方式生成文本。
     *
     * @param unit
     * @param prompt
     * @param option
     * @param history
     * @param participantContact
     * @return
     */
    public GeneratingRecord syncGenerateText(AIGCUnit unit, String prompt, GeneratingOption option,
                                   List<GeneratingRecord> history, Contact participantContact) {
        AtomicInteger count = this.generateTextUnitCountMap.get(unit.getCapability().getName());
        if (null == count) {
            count = new AtomicInteger(1);
            this.generateTextUnitCountMap.put(unit.getCapability().getName(), count);
        }
        else {
            count.incrementAndGet();
        }

        if (this.useAgent) {
            Logger.d(this.getClass(), "#syncGenerateText - Agent - \"" + unit.getCapability().getName() + "\" - history:"
                    + ((null != history) ? history.size() : 0));
            count.decrementAndGet();
            return Agent.getInstance().generateText(Utils.randomString(16),
                    unit.getCapability().getName(), prompt, option, history);
        }

        JSONArray historyArray = new JSONArray();
        if (null != history) {
            for (GeneratingRecord record : history) {
                historyArray.put(record.toJSON());
            }
        }

        Contact participant = (null == participantContact) ?
                unit.getContact() : participantContact;

        long sn = Utils.generateSerialNumber();

        JSONObject data = new JSONObject();
        data.put("unit", unit.getCapability().getName());
        data.put("content", prompt);
        data.put("participant", participant.toCompactJSON());
        data.put("history", historyArray);
        data.put("option", (null == option) ? (new GeneratingOption()).toJSON() : option.toJSON());

        Packet request = new Packet(AIGCAction.TextToText.name, data);
        ActionDialect dialect = this.cellet.transmit(unit.getContext(), request.toDialect(),
                5 * 60 * 1000, sn);
        if (null == dialect) {
            Logger.w(AIGCService.class, "#syncGenerateText - transmit failed, sn:" + sn);
            // 记录故障
            unit.markFailure(AIGCStateCode.UnitError.code, System.currentTimeMillis(), participant.getId());
            count.decrementAndGet();
            return null;
        }

        Packet response = new Packet(dialect);
        JSONObject payload = Packet.extractDataPayload(response);

        String responseText = "";
        String thoughtText = "";
        try {
            responseText = payload.getString("response");
            responseText = responseText.trim();
            thoughtText = payload.getString("thought");
            thoughtText = thoughtText.trim();
        } catch (Exception e) {
            Logger.w(AIGCService.class, "#syncGenerateText - failed, sn:" + sn);
            count.decrementAndGet();
            return null;
        }

        // 计数
        count.decrementAndGet();

        // 过滤中文字符
        responseText = TextUtils.filterChinese(unit, responseText);
        return new GeneratingRecord(request.sn, unit.getCapability().getName(), prompt, responseText, thoughtText);
    }

    @Override
    public GeneratingRecord generateText(String unitName, String prompt, GeneratingOption option,
                                         List<GeneratingRecord> history) {
        return this.syncGenerateText(unitName, prompt, option, history, null);
    }

    /**
     * 生成文本。
     *
     * @param channel
     * @param unit
     * @param query
     * @param prompt
     * @param option
     * @param histories
     * @param maxHistories
     * @param attachments
     * @param categories
     * @param recordable
     * @param listener
     */
    public void generateText(AIGCChannel channel, AIGCUnit unit, String query, String prompt, GeneratingOption option,
                             List<GeneratingRecord> histories, int maxHistories, List<GeneratingRecord> attachments,
                             List<String> categories, boolean recordable, GenerateTextListener listener) {
        if (this.useAgent) {
            unit = Agent.getInstance().selectUnit(unit.getCapability().getName());
        }

        if (null == unit) {
            // 没有单元数据
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.onFailed(channel, AIGCStateCode.NotFound);
                }
            });
            return;
        }

        GenerateTextUnitMeta meta = new GenerateTextUnitMeta(unit, channel, prompt, option, categories,
                histories, attachments, listener);
        meta.setMaxHistories(maxHistories);
        meta.setOriginalQuery(query);
        meta.setRecordHistoryEnabled(recordable);
        meta.setNetworkingEnabled(false);

        if (Logger.isDebugLevel()) {
            if (null != histories) {
                Logger.d(this.getClass(), "#generateText - histories size: " + histories.size());
            }
        }

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

        GeneratingRecord record = channel.getRecord(sn);
        if (null == record) {
            ConversationUnitMeta unitMeta = null;

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

    /*
     * 执行自然语言任务。
     *
     * @param task
     * @param listener
     * @return
     * @deprecated 2024-12-13 废弃
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
    }*/

    /* FIXME 2024-12-13 过时，废弃
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
    }*/

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

        UnitMeta meta = new SummarizationUnitMeta(unit, modified, listener);

        synchronized (this.summarizationQueueMap) {
            Queue<UnitMeta> queue = this.summarizationQueueMap.get(unit.getQueryKey());
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
                    processQueue(meta.unit.getQueryKey(), summarizationQueueMap);
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

        UnitMeta meta = new TextToImageUnitMeta(unit, channel, text, listener);

        synchronized (this.textToImageQueueMap) {
            Queue<UnitMeta> queue = this.textToImageQueueMap.get(unit.getQueryKey());
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
                    processQueue(meta.unit.getQueryKey(), textToImageQueueMap);
                }
            });
        }

        return true;
    }

    /**
     * 文本生成文件。
     *
     * @param channel 频道。
     * @param text 文本内容。
     * @param attachment 附件。
     * @param listener 监听器。
     * @return
     */
    public boolean generateFile(AIGCChannel channel, String text, GeneratingRecord attachment, TextToFileListener listener) {
        if (!this.isStarted()) {
            return false;
        }

        AIGCUnit unit = this.selectUnitByName(ModelConfig.PSYCHOLOGY_UNIT);
        if (null == unit) {
            unit = this.selectUnitByName(ModelConfig.BAIZE_UNIT);
            if (null == unit) {
                Logger.e(this.getClass(), "#generateFile - No unit, token: " + channel.getAuthToken().getCode());
                return false;
            }
        }

        UnitMeta meta = new TextToFileUnitMeta(unit, channel, text, attachment.queryFileLabels, listener);

        synchronized (this.textToFileQueueMap) {
            Queue<UnitMeta> queue = this.textToFileQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.textToFileQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            unit.setRunning(true);

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processQueue(meta.unit.getQueryKey(), textToFileQueueMap);
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

        UnitMeta meta = new ExtractKeywordsUnitMeta(unit, text, listener);

        synchronized (this.extractKeywordsQueueMap) {
            Queue<UnitMeta> queue = this.extractKeywordsQueueMap.get(unit.getQueryKey());
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
                    processQueue(meta.unit.getQueryKey(), extractKeywordsQueueMap);
                }
            });
        }

        return true;
    }

    /**
     * 语义搜索。
     *
     * @param query
     * @param listener
     * @return
     */
    public boolean semanticSearch(String query, SemanticSearchListener listener) {
        if (!this.isStarted()) {
            return false;
        }

        // 查找有该能力的单元
        AIGCUnit unit = this.selectUnitBySubtask(AICapability.NaturalLanguageProcessing.SemanticSearch);
        if (null == unit) {
            Logger.w(AIGCService.class, "No semantic search unit setup in server");
            return false;
        }

        UnitMeta meta = new SemanticSearchUnitMeta(unit, query, listener);

        synchronized (this.semanticSearchQueueMap) {
            Queue<UnitMeta> queue = this.semanticSearchQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.semanticSearchQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            unit.setRunning(true);

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processQueue(meta.unit.getQueryKey(), semanticSearchQueueMap);
                }
            });
        }

        return true;
    }

    /**
     * 检索并重排序。
     *
     * @param queries
     * @param listener
     * @return
     */
    public boolean retrieveReRank(List<String> queries, RetrieveReRankListener listener) {
        if (!this.isStarted()) {
            return false;
        }

        // 查找有该能力的单元
        AIGCUnit unit = this.selectUnitBySubtask(AICapability.NaturalLanguageProcessing.RetrieveReRank);
        if (null == unit) {
            Logger.w(AIGCService.class, "No retrieve re-rank unit setup in server");
            return false;
        }

        UnitMeta meta = new RetrieveReRankUnitMeta(unit, queries, listener);

        synchronized (this.retrieveReRankQueueMap) {
            Queue<UnitMeta> queue = this.retrieveReRankQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.retrieveReRankQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            unit.setRunning(true);

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processQueue(meta.unit.getQueryKey(), retrieveReRankQueueMap);
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
     * 生成心理学绘画测验报告。
     *
     * @param token
     * @param attribute
     * @param fileCode
     * @param theme
     * @param maxIndicatorTexts
     * @param listener
     * @return
     */
    public PaintingReport generatePaintingReport(String token, Attribute attribute, String fileCode,
                                                 Theme theme, int maxIndicatorTexts,
                                                 PaintingReportListener listener) {
        if (!this.isStarted()) {
            return null;
        }

        AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
        AuthToken authToken = authService.getToken(token);
        if (null == authToken) {
            Logger.w(this.getClass(), "#generatePaintingReport - Token error: " + token);
            return null;
        }

        AbstractModule fileStorage = this.getKernel().getModule("FileStorage");
        if (null == fileStorage) {
            Logger.e(this.getClass(), "#generatePaintingReport - File storage service is not ready");
            return null;
        }

        GetFile getFile = new GetFile(authToken.getDomain(), fileCode);
        JSONObject fileLabelJson = fileStorage.notify(getFile);
        if (null == fileLabelJson) {
            Logger.e(this.getClass(), "#generatePaintingReport - Get file failed: " + fileCode);
            return null;
        }

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#generatePaintingReport - max indicator texts: " + maxIndicatorTexts +
                    " , file: " + fileCode);
        }

        FileLabel fileLabel = new FileLabel(fileLabelJson);

        AIGCChannel channel = this.getChannelByToken(token);
        if (null == channel) {
            channel = this.createChannel(token, "Baize", Utils.randomString(16));
        }

        // 生成报告
        PaintingReport report = PsychologyScene.getInstance().generatePredictingReport(channel,
                attribute, fileLabel, theme, maxIndicatorTexts, listener);

        return report;
    }

    /**
     * 生成心理学量表测验报告。
     *
     * @param channel
     * @param scale
     * @param listener
     * @return
     */
    public ScaleReport generateScaleReport(AIGCChannel channel, Scale scale, ScaleReportListener listener) {
        if (!this.isStarted()) {
            return null;
        }

        return PsychologyScene.getInstance().generateScaleReport(channel, scale, listener);
    }

    /**
     * 生成心理学量表测验报告。
     *
     * @param token
     * @param scaleSn
     * @param listener
     * @return
     */
    public ScaleReport generateScaleReport(String token, long scaleSn, ScaleReportListener listener) {
        if (!this.isStarted()) {
            return null;
        }

        AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
        AuthToken authToken = authService.getToken(token);
        if (null == authToken) {
            Logger.w(this.getClass(), "#generateScaleReport - Token error: " + token);
            return null;
        }

        try {
            Scale scale = PsychologyScene.getInstance().getScale(scaleSn);
            if (null == scale) {
                Logger.w(this.getClass(), "#generateScaleReport - No scale, sn: " + scaleSn);
                return null;
            }

            AIGCChannel channel = this.getChannelByToken(token);
            if (null == channel) {
                channel = this.createChannel(token, "Baize", Utils.randomString(16));
            }

            ScaleReport report = PsychologyScene.getInstance().generateScaleReport(channel, scale, listener);
            return report;
        } catch (Exception e) {
            Logger.e(this.getClass(), "#generateScaleReport", e);
            return null;
        }
    }

    public boolean automaticSpeechRecognition(AuthToken authToken, String fileCode, AutomaticSpeechRecognitionListener listener) {
        AbstractModule fileStorage = this.getKernel().getModule("FileStorage");
        if (null == fileStorage) {
            Logger.e(this.getClass(), "#automaticSpeechRecognition - File storage service is not ready");
            return false;
        }

        FileLabel fileLabel = this.getFile(authToken.getDomain(), fileCode);
        if (null == fileLabel) {
            Logger.e(this.getClass(), "#automaticSpeechRecognition - Get file failed: " + fileCode);
            return false;
        }

        // 查找有该能力的单元
        AIGCUnit unit = this.selectUnitBySubtask(AICapability.AudioProcessing.AutomaticSpeechRecognition);
        if (null == unit) {
            Logger.w(this.getClass(), "#automaticSpeechRecognition - No task unit setup in server");
            return false;
        }

        UnitMeta meta = new SpeechRecognitionUnitMeta(unit, fileLabel, listener);

        synchronized (this.speechQueueMap) {
            Queue<UnitMeta> queue = this.speechQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.speechQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            unit.setRunning(true);

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processQueue(meta.unit.getQueryKey(), speechQueueMap);
                }
            });
        }

        return true;
    }

    /**
     * 语音情绪识别。
     *
     * @param token
     * @param fileCode
     * @param listener
     * @return
     */
    public boolean speechEmotionRecognition(AuthToken token, String fileCode, SpeechEmotionRecognitionListener listener) {
        final FileLabel fileLabel = this.getFile(token.getDomain(), fileCode);
        if (null == fileLabel) {
            Logger.w(this.getClass(), "#speechEmotionRecognition - Can NOT find file: " + fileCode);
            return false;
        }

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                AIGCUnit unit = selectUnitByName(ModelConfig.PSYCHOLOGY_UNIT);
                if (null == unit) {
                    Logger.w(AIGCService.class, "#speechEmotionRecognition - No unit");
                    listener.onFailed(fileLabel, AIGCStateCode.UnitNoReady);
                    return;
                }

                // 判断文件类型
                if (fileLabel.getFileType() != FileType.WAV &&
                        fileLabel.getFileType() != FileType.MP3 &&
                        fileLabel.getFileType() != FileType.OGG &&
                        fileLabel.getFileType() != FileType.M4A &&
                        fileLabel.getFileType() != FileType.AMR &&
                        fileLabel.getFileType() != FileType.AAC) {
                    Logger.w(AIGCService.class, "#speechEmotionRecognition - No support file: " + fileLabel.getFileType().getPreferredExtension());
                    listener.onFailed(fileLabel, AIGCStateCode.FileError);
                    return;
                }

                JSONObject payload = new JSONObject();
                payload.put("fileLabel", fileLabel.toJSON());
                Packet request = new Packet(AIGCAction.SpeechEmotionRecognition.name, payload);
                ActionDialect dialect = cellet.transmit(unit.getContext(), request.toDialect(), 3 * 60 * 1000);
                if (null == dialect) {
                    Logger.w(AIGCService.class, "#speechEmotionRecognition - Unit error");
                    // 回调错误
                    listener.onFailed(fileLabel, AIGCStateCode.UnitError);
                    return;
                }

                Packet response = new Packet(dialect);
                JSONObject data = Packet.extractDataPayload(response);
                if (!data.has("result")) {
                    Logger.w(AIGCService.class, "#speechEmotionRecognition - Unit process failed");
                    // 回调错误
                    listener.onFailed(fileLabel, AIGCStateCode.Failure);
                    return;
                }

                SpeechEmotion result = new SpeechEmotion(data.getJSONObject("result"));
                // 回调结束
                listener.onCompleted(fileLabel, result);

                // 写入数据库
                EmotionRecord record = new EmotionRecord(token.getContactId(), result.emotion, EmotionRecord.SOURCE_SPEECH);
                record.sourceData = fileLabel.toCompactJSON();
                if (!storage.writeEmotionRecord(record)) {
                    Logger.e(AIGCService.class, "#speechEmotionRecognition - Write emotion record error: " +
                            fileCode);
                }
            }
        });

        return true;
    }

    public List<EmotionRecord> getEmotionRecords(AuthToken authToken) {
        return this.storage.readEmotionRecords(authToken.getContactId());
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

    public File loadFile(String domain, String fileCode) {
        AbstractModule fileStorage = this.getKernel().getModule("FileStorage");
        if (null == fileStorage) {
            Logger.e(this.getClass(), "#loadFile - File storage service is not ready");
            return null;
        }

        LoadFile loadFile = new LoadFile(domain, fileCode);
        try {
            String path = fileStorage.notify(loadFile);
            return new File(path);
        } catch (Exception e) {
            Logger.e(this.getClass(), "#loadFile - File storage service load failed", e);
            return null;
        }
    }

    public FileLabel saveFile(AuthToken authToken, String fileCode, File file, String filename, boolean delete) {
        AbstractModule fileStorage = this.getKernel().getModule("FileStorage");
        if (null == fileStorage) {
            Logger.e(this.getClass(), "#saveFile - File storage service is not ready");
            return null;
        }

        // 创建文件标签
        FileLabel fileLabel = FileUtils.makeFileLabel(authToken.getDomain(), fileCode, authToken.getContactId(), file);
        if (null != filename) {
            fileLabel.setFileName(filename);
        }

        try {
            JSONObject fileJson = fileStorage.notify(new SaveFile(file.getAbsolutePath(), fileLabel));
            return new FileLabel(fileJson);
        } catch (Exception e) {
            Logger.e(this.getClass(), "#saveFile - File storage service save failed", e);
            return null;
        } finally {
            // 删除临时文件
            if (delete && file.exists()) {
                try {
                    file.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
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
     * 句子相似度。
     *
     * @param sentenceA
     * @param sentenceB
     * @return
     */
    public double sentenceSimilarity(String sentenceA, String sentenceB) {
        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(this.tokenizer);
        List<String> wordsA = analyzer.analyzeOnlyWords(sentenceA, 10);
        List<String> wordsB = analyzer.analyzeOnlyWords(sentenceB, 10);
        List<String> pole = null;
        List<String> monkey = null;
        if (wordsA.size() > wordsB.size()) {
            pole = wordsA;
            monkey = wordsB;
        }
        else {
            pole = wordsB;
            monkey = wordsA;
        }
        double count = 0;
        for (String word : pole) {
            if (monkey.contains(word)) {
                count += 1.0;
            }
        }
        return count / pole.size();
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
     * 识别上下文数据。
     *
     * @param text
     * @param authToken
     * @return
     */
    private ComplexContext recognizeContext(String text, AuthToken authToken) {
        final String content = text.trim();
        ComplexContext result = new ComplexContext();

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
                result = new ComplexContext(false);
                for (String url : urlList) {
                    HyperlinkResource resource = new HyperlinkResource(url, HyperlinkResource.TYPE_FAILURE);
                    resource.fixContent();
                    result.addResource(resource);
                }
            }
            else {
                JSONObject data = Packet.extractDataPayload(response);
                JSONArray list = data.getJSONArray("list");
                result = new ComplexContext(false);
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
                result = new ComplexContext(false);
                result.addResource(resource);
            }
            else {
                JSONObject data = Packet.extractDataPayload(response);
                JSONArray list = data.getJSONArray("list");
                if (list.isEmpty()) {
                    // 列表没有数据，获取 URL 失败
                    result = new ComplexContext(false);
                    HyperlinkResource resource = new HyperlinkResource(content, HyperlinkResource.TYPE_FAILURE);
                    resource.fixContent();
                    result.addResource(resource);
                }
                else {
                    result = new ComplexContext(false);
                    JSONObject resPayload = new JSONObject();
                    resPayload.put("payload", list.getJSONObject(0));
                    HyperlinkResource resource = new HyperlinkResource(resPayload);
                    resource.fixContent();
                    result.addResource(resource);
                }
            }
        }
        else {
            Stage stage = Explorer.getInstance().perform(authToken, content);
            if (stage.isFlowable()) {
                result = new ComplexContext(false);
                result.stage = stage;
            }

            /*Stage stage = Explorer.getInstance().infer(content);
            if (stage.isComplex()) {
                result = new ComplexContext(ComplexContext.Type.Heavyweight);

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
            }*/
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

            AtomicInteger count = this.generateTextUnitCountMap.get(meta.unit.getCapability().getName());
            if (null == count) {
                count = new AtomicInteger(1);
                this.generateTextUnitCountMap.put(meta.unit.getCapability().getName(), count);
            }
            else {
                count.incrementAndGet();
            }

            // 执行处理
            try {
                meta.process();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#processGenerateTextQueue - meta process", e);
            }

            count.decrementAndGet();

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

    private void processQueue(String queryKey, Map<String, Queue<UnitMeta>> queueMap) {
        Queue<UnitMeta> queue = queueMap.get(queryKey);
        if (null == queue) {
            Logger.w(AIGCService.class, "#processQueue - No found unit: " + queryKey);
            return;
        }

        UnitMeta meta = queue.poll();
        while (null != meta) {
            // 执行处理
            try {
                meta.process();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#processQueue - meta process", e);
            }

            meta = queue.poll();
        }

        AIGCUnit unit = this.unitMap.get(queryKey);
        if (null != unit) {
            unit.setRunning(false);
        }
    }

    /*private void processSummarizationQueue(String queryKey) {
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
    }*/

    /*private void processTextToImageQueue(String queryKey) {
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
    }*/

    /*private void processExtractKeywordsQueue(String queryKey) {
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
    }*/

    /*private void processASRQueue(String queryKey) {
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
    }*/

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


    protected abstract class UnitMeta {

        protected AIGCUnit unit;

        public UnitMeta(AIGCUnit unit) {
            this.unit = unit;
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

        protected AIGCChannel channel;

        protected Contact participant;

        protected String content;

        protected String originalQuery;

        protected GeneratingOption option;

        protected List<String> categories;

        protected List<GeneratingRecord> histories;

        protected int maxHistories;

        protected List<GeneratingRecord> attachments;

        protected GenerateTextListener listener;

        protected AIGCChatHistory history;

        protected boolean recordHistoryEnabled = true;

        protected boolean networkingEnabled = false;

        public GenerateTextUnitMeta(AIGCUnit unit, AIGCChannel channel, String content, GeneratingOption option,
                                    List<String> categories,
                                    List<GeneratingRecord> histories,
                                    List<GeneratingRecord> attachments,
                                    GenerateTextListener listener) {
            super(unit);
            this.sn = Utils.generateSerialNumber();
            this.channel = channel;
            this.participant = ContactManager.getInstance().getContact(channel.getAuthToken().getDomain(),
                    channel.getAuthToken().getContactId());
            this.content = content;
            this.option = option;
            this.categories = categories;
            this.histories = histories;
            this.attachments = attachments;
            this.maxHistories = 0;
            this.listener = listener;

            this.history = new AIGCChatHistory(this.sn, this.channel.getCode(), unit.getCapability().getName(),
                    this.participant.getDomain().getName());
            this.history.queryContactId = channel.getAuthToken().getContactId();
            this.history.queryTime = System.currentTimeMillis();
            this.history.queryContent = content;
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

        public void setMaxHistories(int max) {
            this.maxHistories = max;
        }

        @Override
        public void process() {
            this.channel.setLastUnitMetaSn(this.sn);

            // 识别内容
            ComplexContext complexContext = option.recognizeContext ?
                    recognizeContext(this.content, this.channel.getAuthToken()) :
                    new ComplexContext();

            // 设置是否进行联网分析
            complexContext.setNetworking(this.networkingEnabled);

            GeneratingRecord result = null;

            final StringBuilder realPrompt = new StringBuilder(this.content);

            if (complexContext.isSimplified()) {
                // 一般文本

                int recommendHistories = 5;

                // 提示词长度限制
                int lengthLimit = ModelConfig.getPromptLengthLimit(this.unit.getCapability().getName());
                lengthLimit -= this.content.length();

                JSONObject data = new JSONObject();
                data.put("unit", this.unit.getCapability().getName());
                data.put("content", this.content);
                data.put("participant", this.participant.toCompactJSON());
                data.put("option", this.option.toJSON());

                boolean useQueryAttachment = false;
                if (null != this.attachments) {
                    for (GeneratingRecord record : this.attachments) {
                        if (record.hasQueryFile()) {
                            if (null == this.history.queryFileLabels) {
                                this.history.queryFileLabels = new ArrayList<>();
                            }
                            this.history.queryFileLabels.addAll(record.queryFileLabels);
                        }

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

                    for (GeneratingRecord record : this.attachments) {
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
                } // End useQueryAttachment

                // 处理多轮历史记录
                int lengthCount = data.getString("content").length();
                List<GeneratingRecord> candidateRecords = new ArrayList<>();
                if (null == this.histories) {
                    int validNumHistories = this.maxHistories;
                    if (validNumHistories > 0) {
                        List<GeneratingRecord> records = this.channel.getLastHistory(validNumHistories);
                        // 正序列表转为倒序以便计算上下文长度
                        Collections.reverse(records);
                        for (GeneratingRecord record : records) {
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
                    for (int i = 0; i < this.histories.size(); ++i) {
                        GeneratingRecord record = this.histories.get(i);
                        if (record.hasQueryFile() || record.hasQueryAddition()) {
                            // 为了兼容旧版本，排除掉附件类型
                            continue;
                        }

                        lengthCount += record.totalWords();
                        // 判断长度
                        if (lengthCount > lengthLimit) {
                            // 长度越界
                            break;
                        }
                        // 加入候选
                        candidateRecords.add(record);
                        if (candidateRecords.size() >= recommendHistories) {
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

                // 写入多轮对话历史数组
                JSONArray history = new JSONArray();
                for (GeneratingRecord record : candidateRecords) {
                    history.put(record.toJSON());
                }
                data.put("history", history);

                if (this.content.contains(Consts.NO_CONTENT_SENTENCE) || Consts.NO_CONTENT_SENTENCE.contains(this.content)) {
                    // 知识库会使用 NO_CONTENT_SENTENCE 作为答案
                    String responseText = Consts.NO_CONTENT_SENTENCE;
                    result = this.channel.appendRecord(this.sn, this.unit.getCapability().getName(),
                            (null != this.originalQuery) ? this.originalQuery : this.content,
                            responseText, "", complexContext);
                }
                else if (this.networkingEnabled) {
                    // 启用搜索或者启用联网信息检索都执行搜索
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            // 进行资源搜索
                            SearchResult searchResult = Explorer.getInstance().search(
                                    (null != originalQuery) ? originalQuery : content, channel.getAuthToken());
                            if (searchResult.hasResult()) {
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

                    String responseText = Consts.SEARCHING_INTERNET_FOR_INFORMATION;
                    result = this.channel.appendRecord(this.sn, this.unit.getCapability().getName(),
                            (null != this.originalQuery) ? this.originalQuery : this.content,
                            responseText, "", complexContext);
                }
                else if (useAgent) {
                    GeneratingRecord generatingRecord =
                            Agent.getInstance().generateText(channel.getCode(), this.unit.getCapability().getName(),
                                    this.content, new GeneratingOption(), this.histories);
                    if (null != generatingRecord) {
                        // 过滤中文字符
                        result = this.channel.appendRecord(this.sn, this.unit.getCapability().getName(),
                                (null != this.originalQuery) ? this.originalQuery : this.content,
                                generatingRecord.answer, generatingRecord.thought, complexContext);
                    }
                    else {
                        this.channel.setProcessing(false);
                        // 回调失败
                        this.listener.onFailed(this.channel, AIGCStateCode.UnitError);
                        return;
                    }
                }
                else {
                    Packet request = new Packet(AIGCAction.TextToText.name, data);
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
                    String thoughtText = "";
                    try {
                        responseText = payload.getString("response");
                        thoughtText = payload.getString("thought");
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
                            responseText.trim(), thoughtText.trim(), complexContext);
                }
            }
            else {
                // 复合型数据
                if (complexContext.stage.isFlowable()) {
                    GeneratingRecord record = complexContext.stage.flowable.generate(AIGCService.this);
                    result = this.channel.appendRecord(this.sn, record);
                }
                else {
                    ResourceAnswer resourceAnswer = new ResourceAnswer(complexContext);
                    // 提取内容
                    String content = resourceAnswer.extractContent(AIGCService.this, this.channel.getAuthToken());
                    String answer = resourceAnswer.answer(content);
                    result = this.channel.appendRecord(this.sn, this.unit.getCapability().getName(),
                            (null != this.originalQuery) ? this.originalQuery : this.content, answer.trim(), "",
                            complexContext);
                }
            }

            if (complexContext.isSimplified()) {
                if (this.networkingEnabled) {
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
            this.history.thought = result.thought;

            // 设置上下文
            this.history.context = complexContext;

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
                        storage.writeHistory(history);
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
                    GeneratingRecord answer = syncGenerateText(this.channel.getAuthToken(), ModelConfig.BAIZE_X_UNIT, prompt,
                            new GeneratingOption());
                    if (null != answer) {
                        // 记录内容
                        pageContent.append(answer.answer);
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
            GeneratingRecord result = syncGenerateText(this.channel.getAuthToken(), unitName, prompt, new GeneratingOption());
            if (null == result) {
                Logger.w(this.getClass(), "#performSearchPageQA - Infers page content failed, cid:"
                        + this.channel.getAuthToken().getContactId());
                // 使用 null 值填充
                context.fixNetworkingResult(null, null);
                return;
            }

            if (Logger.isDebugLevel()) {
                Logger.d(this.getClass(), "#performSearchPageQA - Result length: " + result.answer.length());
            }

            // 将页面推理结果填充到上下文
            context.fixNetworkingResult(pages, result.answer);
        }

        protected void fillRecords(List<GeneratingRecord> recordList, List<String> categories, int lengthLimit,
                                   String unitName) {
            int total = 0;
            for (String category : categories) {
                List<KnowledgeParaphrase> list = storage.readKnowledgeParaphrases(category);
                for (KnowledgeParaphrase paraphrase : list) {
                    total += paraphrase.getWord().length() + paraphrase.getParaphrase().length();
                    if (total > lengthLimit) {
                        break;
                    }

                    GeneratingRecord record = new GeneratingRecord(unitName,
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
            public void onGenerated(AIGCChannel channel, GeneratingRecord record) {
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
            super(unit, channel, content, parameter.toGenerativeOption(), parameter.categories,
                    parameter.records, null, null);
            this.listener = this.generateListener;
            this.maxHistories = parameter.histories;
            this.recordHistoryEnabled = parameter.recordable;
            this.networkingEnabled = parameter.networking;
            this.parameter = parameter;
            this.conversationListener = listener;
        }
    }

    private class SemanticSearchUnitMeta extends UnitMeta {

        private String query;

        private SemanticSearchListener listener;

        public SemanticSearchUnitMeta(AIGCUnit unit, String query, SemanticSearchListener listener) {
            super(unit);
            this.query = query;
            this.listener = listener;
        }

        @Override
        public void process() {
            JSONObject data = new JSONObject();
            data.put("query", this.query);
            Packet request = new Packet(AIGCAction.SemanticSearch.name, data);
            ActionDialect dialect = cellet.transmit(this.unit.getContext(), request.toDialect());
            if (null == dialect) {
                Logger.w(AIGCService.class, "The semantic search unit error");
                // 回调错误
                this.listener.onFailed(this.query, AIGCStateCode.UnitError);
                return;
            }

            List<QuestionAnswer> qaList = new ArrayList<>();

            Packet response = new Packet(dialect);
            JSONObject payload = Packet.extractDataPayload(response);
            JSONArray resultList = payload.getJSONArray("result");
            for (int i = 0; i < resultList.length(); ++i) {
                QuestionAnswer qa = new QuestionAnswer(resultList.getJSONObject(i));
                qaList.add(qa);
            }

            this.listener.onCompleted(this.query, qaList);
        }
    }

    private class RetrieveReRankUnitMeta extends UnitMeta {

        private List<String> queries;

        private RetrieveReRankListener listener;

        public RetrieveReRankUnitMeta(AIGCUnit unit, List<String> queries, RetrieveReRankListener listener) {
            super(unit);
            this.queries = queries;
            this.listener = listener;
        }

        @Override
        public void process() {
            JSONObject data = new JSONObject();
            data.put("queries", JSONUtils.toStringArray(this.queries));
            Packet request = new Packet(AIGCAction.RetrieveReRank.name, data);
            ActionDialect dialect = cellet.transmit(this.unit.getContext(), request.toDialect());
            if (null == dialect) {
                Logger.w(AIGCService.class, "The retrieve re-rank unit error");
                // 回调错误
                this.listener.onFailed(this.queries, AIGCStateCode.UnitError);
                return;
            }

            List<RetrieveReRankResult> list = new ArrayList<>();

            Packet response = new Packet(dialect);
            if (Packet.extractCode(response) != AIGCStateCode.Ok.code) {
                Logger.w(AIGCService.class, "The retrieve re-rank unit failed: " + Packet.extractCode(response));
                // 回调错误
                this.listener.onFailed(this.queries, AIGCStateCode.Failure);
                return;
            }
            JSONObject payload = Packet.extractDataPayload(response);
            JSONArray resultList = payload.getJSONArray("result");
            for (int i = 0; i < resultList.length(); ++i) {
                RetrieveReRankResult result = new RetrieveReRankResult(resultList.getJSONObject(i));
                list.add(result);
            }

            this.listener.onCompleted(list);
        }
    }

    /*private class NaturalLanguageTaskMeta {

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
    }*/

    /*private class SentimentUnitMeta {

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
    }*/

    private class TextToImageUnitMeta extends UnitMeta {

        protected long sn;

        protected AIGCChannel channel;

        protected String text;

        protected FileLabel fileLabel;

        protected TextToImageListener listener;

        protected AIGCChatHistory history;

        public TextToImageUnitMeta(AIGCUnit unit, AIGCChannel channel, String text, TextToImageListener listener) {
            super(unit);
            this.sn = Utils.generateSerialNumber();
            this.unit = unit;
            this.channel = channel;
            this.text = text;
            this.listener = listener;

            this.history = new AIGCChatHistory(this.sn, channel.getCode(), unit.getCapability().getName(),
                    channel.getDomain().getName());
            this.history.queryContactId = channel.getAuthToken().getContactId();
            this.history.queryTime = System.currentTimeMillis();
            this.history.queryContent = text;
        }

        @Override
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
                GeneratingRecord record = this.channel.appendRecord(this.sn, this.unit.getCapability().getName(),
                        this.text, this.fileLabel);
                this.listener.onCompleted(record);

                // 填写历史
                this.history.answerContactId = this.unit.getContact().getId();
                this.history.answerTime = System.currentTimeMillis();
                this.history.answerContent = this.fileLabel.toCompactJSON().toString();

                this.history.answerFileLabels = new ArrayList<>();
                this.history.answerFileLabels.add(this.fileLabel);

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
                        storage.writeHistory(history);
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

    private class TextToFileUnitMeta extends UnitMeta {

        private long sn;

        private AIGCChannel channel;

        private String text;

        private List<FileLabel> sources;

        private TextToFileListener listener;

        public TextToFileUnitMeta(AIGCUnit unit, AIGCChannel channel, String text, List<FileLabel> sources, TextToFileListener listener) {
            super(unit);
            this.sn = Utils.generateSerialNumber();
            this.channel = channel;
            this.text = text;
            this.sources = new ArrayList<>(sources);
            this.listener = listener;
        }

        @Override
        public void process() {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.onProcessing(channel);
                }
            });

            int promptLimit = ModelConfig.getPromptLengthLimit(this.unit.getCapability().getName());

            List<FileLabel> queryFiles = new ArrayList<>();
            StringBuilder answerBuf = new StringBuilder("已处理文件");
            long processedSize = 0;
            long generatingSize = 0;

            FileType fileType = FileType.UNKNOWN;
            JSONArray files = new JSONArray();
            for (FileLabel fileLabel : this.sources) {
                fileType = fileLabel.getFileType();
                if (fileType == FileType.XLSX || fileType == FileType.XLS) {
                    files.put(fileLabel.toJSON());
                    queryFiles.add(fileLabel);

                    answerBuf.append("“**").append(fileLabel.getFileName()).append("**”，");
                    processedSize += fileLabel.getFileSize();
                }
            }
            answerBuf.delete(answerBuf.length() - 1, answerBuf.length());
            answerBuf.append("。");

            ActionDialect dialect = null;
            if (fileType == FileType.XLSX || fileType == FileType.XLS) {
                JSONObject data = new JSONObject();
                data.put("files", files);
                Packet request = new Packet(FileProcessorAction.ReadExcel.name, data);
                dialect = cellet.transmit(this.unit.getContext(), request.toDialect(), 60 * 1000, this.sn);
                if (null == dialect) {
                    Logger.w(this.getClass(), "File processor service error");
                    // 回调错误
                    this.listener.onFailed(this.channel, AIGCStateCode.UnitError);
                    return;
                }
            }
            else {
                Logger.w(this.getClass(), "Unsupported file type： " + fileType.getPreferredExtension());
                // 回调错误
                this.listener.onFailed(this.channel, AIGCStateCode.IllegalOperation);
                return;
            }

            Packet response = new Packet(dialect);
            JSONObject payload = Packet.extractDataPayload(response);
            JSONArray fileResult = payload.getJSONArray("result");
            if (fileResult.length() == 0) {
                Logger.w(this.getClass(), "File error");
                // 回调错误
                this.listener.onFailed(this.channel, AIGCStateCode.FileError);
                return;
            }

            String notice = null;
            GeneratingRecord result = new GeneratingRecord(ModelConfig.BAIZE_UNIT, this.text, answerBuf.toString());
            result.queryFileLabels = queryFiles;

            for (int i = 0; i < fileResult.length(); ++i) {
                JSONObject fileData = fileResult.getJSONObject(i);
                String fileCode = fileData.getString("fileCode");
                JSONArray sheets = fileData.getJSONArray("sheets");
                for (int n = 0; n < sheets.length(); ++n) {
                    JSONObject sheetJson = sheets.getJSONObject(n);
                    String name = sheetJson.getString("name");
                    String content = sheetJson.getString("content");

                    if (content.length() > promptLimit) {
                        Logger.w(this.getClass(), "#process - Content length exceeded the limit: " +
                                content.length() + "/" + promptLimit);
                        continue;
                    }

                    StringBuilder prompt = new StringBuilder();
                    prompt.append("已知以下表格数据：\n\n");
                    prompt.append("表格名称：").append(name).append("\n\n");
                    prompt.append("表格数据内容：\n\n").append(content);
                    prompt.append("\n\n");
                    prompt.append(String.format(Consts.PROMPT_SUFFIX_FORMAT, this.text));

                    GeneratingRecord generating = syncGenerateText(ModelConfig.BAIZE_X_UNIT, prompt.toString(),
                            null, null, null);
                    if (null == generating) {
                        Logger.w(this.getClass(), "#process - Generating failed: " + fileCode);
                        continue;
                    }

                    String answer = generating.answer;

                    String tmpNotice = this.extractNotice(answer);
                    String csv = TextUtils.extractMarkdownTable(answer);

                    if (null != tmpNotice) {
                        notice = tmpNotice;
                    }

                    if (null != csv) {
                        generatingSize += csv.length();

                        // 文件码
                        String tmpFileCode = FileUtils.makeFileCode(fileCode, channel.getAuthToken().getDomain(), name);
                        Path path = Paths.get(workingPath.getAbsolutePath(), tmpFileCode + ".csv");
                        try {
                            // 写入文件
                            Files.write(path, csv.getBytes(StandardCharsets.UTF_8));
                        } catch (IOException e) {
                            Logger.e(this.getClass(), "#process - File write failed: " + path.toString(), e);
                        }

                        FileLabel fileLabel = saveFile(this.channel.getAuthToken(),
                                tmpFileCode, path.toFile(), name + ".csv", true);
                        if (null != fileLabel) {
                            result.addAnswerFileLabel(fileLabel);
                        }
                        else {
                            Logger.e(this.getClass(), "#process - Save file failed: " + path.toString());
                        }
                    }
                    else {
                        Logger.e(this.getClass(), "#process - Extract markdown table failed: " + fileCode);
                    }
                }
            }

            if (null != result.answerFileLabels) {
                answerBuf.append("处理的数据大小合计 ").append(FileUtils.scaleFileSize(processedSize)).append(" 。");
                answerBuf.append("生成").append(result.answerFileLabels.size()).append("个文件，合计 ");
                answerBuf.append(FileUtils.scaleFileSize(generatingSize)).append(" 。\n");
            }
            else {
                answerBuf.append("处理的数据大小合计 ").append(FileUtils.scaleFileSize(processedSize)).append(" 。");
                answerBuf.append("但是未能正确读取数据，建议检查一下待处理文件。");
            }

            if (null != notice) {
                answerBuf.append("\n").append(notice);
            }

            result.answer = answerBuf.toString();
            listener.onCompleted(result);
        }

        private String extractNotice(String text) {
            String[] lines = text.split("\n");
            for (String line : lines) {
                if (line.length() < 2) {
                    continue;
                }

                if (line.contains("注意")) {
                    return line;
                }
            }
            return null;
        }
    }

    private class SummarizationUnitMeta extends UnitMeta {

        protected String text;

        protected SummarizationListener listener;

        public SummarizationUnitMeta(AIGCUnit unit, String text, SummarizationListener listener) {
            super(unit);
            this.text = text;
            this.listener = listener;
        }

        @Override
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

    private class ExtractKeywordsUnitMeta extends UnitMeta {

        protected String text;

        protected ExtractKeywordsListener listener;

        public ExtractKeywordsUnitMeta(AIGCUnit unit, String text, ExtractKeywordsListener listener) {
            super(unit);
            this.text = text;
            this.listener = listener;
        }

        @Override
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


    private class SpeechRecognitionUnitMeta extends UnitMeta {

        protected final static String PROMPT = "合理使用标点符号给以下文本断句并修正错别字：%s";

        protected FileLabel file;

        protected AutomaticSpeechRecognitionListener listener;

        public SpeechRecognitionUnitMeta(AIGCUnit unit, FileLabel file, AutomaticSpeechRecognitionListener listener) {
            super(unit);
            this.file = file;
            this.listener = listener;
        }

        @Override
        public void process() {
            JSONObject data = new JSONObject();
            data.put("fileLabel", this.file.toJSON());
            Packet request = new Packet(AIGCAction.AutomaticSpeechRecognition.name, data);
            ActionDialect dialect = cellet.transmit(this.unit.getContext(), request.toDialect(), 5 * 60 * 1000);
            if (null == dialect) {
                Logger.w(AIGCService.class, "#process - Speech unit error: " + this.file.getFileCode());
                // 回调错误
                this.listener.onFailed(this.file, AIGCStateCode.UnitError);
                return;
            }

            Packet response = new Packet(dialect);
            if (AIGCStateCode.Ok.code != Packet.extractCode(response)) {
                Logger.w(AIGCService.class, "#process - Speech unit failed: " + this.file.getFileCode());
                this.listener.onFailed(this.file, AIGCStateCode.Failure);
                return;
            }

            JSONObject payload = Packet.extractDataPayload(response);
            SpeechRecognitionInfo info = new SpeechRecognitionInfo(payload.getJSONObject("result"));

            // 进行标点断句
            String prompt = String.format(PROMPT, info.getText());
            AIGCUnit unit = selectUnitByName(ModelConfig.BAIZE_UNIT);
            if (null != unit) {
                GeneratingRecord result = syncGenerateText(unit, prompt, null, null, null);
                if (null != result) {
                    info.setText(result.answer);
                }
            }
            this.listener.onCompleted(this.file, info);
        }
    }
}
