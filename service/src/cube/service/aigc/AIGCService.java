/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.CachedQueueExecutor;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.*;
import cube.aigc.app.Notification;
import cube.aigc.complex.attachment.Attachment;
import cube.aigc.complex.widget.Event;
import cube.aigc.complex.widget.EventResult;
import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.ScaleReport;
import cube.aigc.psychology.Theme;
import cube.aigc.psychology.composition.Scale;
import cube.auth.AuthConsts;
import cube.auth.AuthToken;
import cube.common.Language;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.*;
import cube.common.notice.DeleteFile;
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
import cube.service.aigc.scene.*;
import cube.service.aigc.unit.*;
import cube.service.auth.AuthService;
import cube.service.auth.AuthServiceHook;
import cube.service.contact.ContactHook;
import cube.service.contact.ContactManager;
import cube.service.contact.ContactMask;
import cube.service.contact.MembershipSystem;
import cube.service.tokenizer.Tokenizer;
import cube.service.tokenizer.keyword.TFIDFAnalyzer;
import cube.storage.StorageType;
import cube.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
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
//    private final Map<String, Queue<GenerateTextUnitMeta>> generateQueueMap;

    /**
     * Key 是 AIGC 的 Query Key
     */
//    private final Map<String, Queue<ConversationUnitMeta>> conversationQueueMap;

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
     * Key 是 AIGC 的 Query Key
     */
    private final Map<String, LinkedList<UnitMeta>> audioQueueMap;

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
    public final File workingPath = new File("storage/tmp/");

    /**
     * 生成文本任务执行实时计数。
     */
    private ConcurrentHashMap<String, AtomicInteger> generateTextUnitCountMap;

    /**
     * 是否访问，仅用于本地测试
     */
    public boolean useAgent = false;

    /**
     * 配置文件最后修改时间。
     */
    private long configFileLastModified = 0;
    // 配置文件上一次检测事件
    private long configFileLastTime = 0;

    private long lastResetUnitTime = 0;

    public AIGCService(AIGCCellet cellet) {
        this.cellet = cellet;
        this.unitMap = new ConcurrentHashMap<>();
        this.unitWeightMap = new ConcurrentHashMap<>();
        this.channelMap = new ConcurrentHashMap<>();
//        this.generateQueueMap = new ConcurrentHashMap<>();
//        this.conversationQueueMap = new ConcurrentHashMap<>();
        this.textToFileQueueMap = new ConcurrentHashMap<>();
        this.textToImageQueueMap = new ConcurrentHashMap<>();
        this.summarizationQueueMap = new ConcurrentHashMap<>();
        this.extractKeywordsQueueMap = new ConcurrentHashMap<>();
        this.semanticSearchQueueMap = new ConcurrentHashMap<>();
        this.retrieveReRankQueueMap = new ConcurrentHashMap<>();
        this.speechQueueMap = new ConcurrentHashMap<>();
        this.audioQueueMap = new ConcurrentHashMap<>();
        this.generateTextUnitCountMap = new ConcurrentHashMap<>();
        this.tokenizer = new Tokenizer();
    }

    @Override
    public void start() {
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

                // 应用事件
                AppEventPlugin appEventPlugin = new AppEventPlugin(AIGCService.this);
                pluginSystem.register(AIGCHook.AppEvent, appEventPlugin);

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

                // 咨询管理器
                CounselingManager.getInstance().start(AIGCService.this);

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

        CounselingManager.getInstance().stop();

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

        if (now - this.lastResetUnitTime > 5 * 60 * 1000) {
            this.lastResetUnitTime = now;
            unitIter = this.unitMap.values().iterator();
            while (unitIter.hasNext()) {
                unitIter.next().resetRunning();
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

        CounselingManager.getInstance().onTick(now);
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

            // 性能配置
            if (null == this.executor) {
                int max = 8;
                try {
                    max = Integer.parseInt(properties.getProperty("threadpool.max", "32"));
                } catch (Exception e) {
                    // Nothing
                }
                if (properties.getProperty("threadpool.type", "fixed").equalsIgnoreCase("cached")) {
                    this.executor = CachedQueueExecutor.newCachedQueueThreadPool(max);
                    Logger.i(this.getClass(), "AI Service - Thread pool type: cached - max: " + max);
                }
                else {
                    this.executor = Executors.newFixedThreadPool(max);
                    Logger.i(this.getClass(), "AI Service - Thread pool type: fixed - max: " + max);
                }
            }

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
//                this.generateQueueMap.remove(unit.getQueryKey());
//                this.conversationQueueMap.remove(unit.getQueryKey());
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
            if (unit.getCapability().getName().equals(unitName)
                    && unit.getContext().isValid()) {
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
    public synchronized AIGCUnit selectIdleUnitByName(String unitName) {
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

        // 按照最近执行时间戳从低到高排序
        Collections.sort(candidates, new Comparator<AIGCUnit>() {
            @Override
            public int compare(AIGCUnit u1, AIGCUnit u2) {
                return (int)(u1.getLastRunningTimestamp() - u2.getLastRunningTimestamp());
            }
        });

        Logger.d(this.getClass(), "#selectIdleUnitByName - Unit: " + unitName + "@"
                + candidates.get(0).getContact().getId());
        return candidates.get(0);
    }

    public synchronized AIGCUnit selectUnitByName(String unitName) {
        AIGCUnit idleUnit = this.selectIdleUnitByName(unitName);
        if (null != idleUnit) {
            return idleUnit;
        }

        ArrayList<AIGCUnit> candidates = new ArrayList<>();

        // 选择所有可用节点
        Iterator<AIGCUnit> iter = this.unitMap.values().iterator();
        while (iter.hasNext()) {
            AIGCUnit unit = iter.next();
            if (unit.getCapability().getName().equals(unitName) &&
                    unit.getContext().isValid()) {
                candidates.add(unit);
            }
        }

        // 如果可用节点超过3个，删除故障数最多的节点
//        if (candidates.size() >= 3) {
//            AIGCUnit excluded = null;
//            int maxNumFailure = -1;
//            for (AIGCUnit unit : candidates) {
//                if (unit.numFailure() > 0) {
//                    if (unit.numFailure() > maxNumFailure) {
//                        maxNumFailure = unit.numFailure();
//                        excluded = unit;
//                    }
//                }
//            }
//            if (null != excluded) {
//                candidates.remove(excluded);
//            }
//        }

        // 无候选节点
        if (candidates.isEmpty()) {
            return null;
        }

        if (candidates.size() == 1) {
            Logger.d(this.getClass(), "#selectUnitByName - Unit: " + unitName + "@"
                    + candidates.get(0).getContact().getId());
            return candidates.get(0);
        }

        // 按照发生错误的数量从低到高排序
//        Collections.sort(candidates, new Comparator<AIGCUnit>() {
//            @Override
//            public int compare(AIGCUnit u1, AIGCUnit u2) {
//                return u1.numFailure() - u2.numFailure();
//            }
//        });

        // 按照权重从高到低排序
//        Collections.sort(candidates, new Comparator<AIGCUnit>() {
//            @Override
//            public int compare(AIGCUnit u1, AIGCUnit u2) {
//                return (int)(u2.getWeight() - u1.getWeight());
//            }
//        });

        // 按照最近执行时间戳从低到高排序
        Collections.sort(candidates, new Comparator<AIGCUnit>() {
            @Override
            public int compare(AIGCUnit u1, AIGCUnit u2) {
                return (int)(u1.getLastRunningTimestamp() - u2.getLastRunningTimestamp());
            }
        });

        // 先进行一次选择，选择最久没有执行的
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
            // 所有单元都在运行
            Logger.d(this.getClass(), "#selectUnitByName - Unit: " + unitName + "@"
                    + unit.getContact().getId());
            return unit;
        }

        // 总是返回最久的单元
        Logger.d(this.getClass(), "#selectUnitByName - Unit: " + unitName + "@"
                + candidates.get(0).getContact().getId());
        return candidates.get(0);
    }

    public synchronized AIGCUnit selectUnitByName(String unitName, long cid) {
        if (cid > 9999999999L) {
            // 10位以上ID进行一般选择
            return this.selectUnitByName(unitName);
        }

        ArrayList<AIGCUnit> candidates = new ArrayList<>();

        // 选择所有权重大于5.0的可用节点
        Iterator<AIGCUnit> iter = this.unitMap.values().iterator();
        while (iter.hasNext()) {
            AIGCUnit unit = iter.next();
            if (unit.getCapability().getName().equals(unitName) &&
                    unit.getContext().isValid() &&
                    unit.getWeight() > 5.0) {
                candidates.add(unit);
            }
        }

        // 无候选节点
        if (candidates.isEmpty()) {
            // 进行一般选择
            return this.selectUnitByName(unitName);
        }

        if (candidates.size() == 1) {
            Logger.d(this.getClass(), "#selectUnitByName - Unit: " + unitName + "@"
                    + candidates.get(0).getContact().getId());
            return candidates.get(0);
        }

        // 按照最近执行时间戳从低到高排序
        Collections.sort(candidates, new Comparator<AIGCUnit>() {
            @Override
            public int compare(AIGCUnit u1, AIGCUnit u2) {
                return (int)(u1.getLastRunningTimestamp() - u2.getLastRunningTimestamp());
            }
        });

        // 先进行一次选择，选择最久没有执行的
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
            // 所有单元都在运行
            Logger.d(this.getClass(), "#selectUnitByName - Unit: " + unitName + "@"
                    + unit.getContact().getId());
            return unit;
        }

        // 总是返回最久的单元
        Logger.d(this.getClass(), "#selectUnitByName - Unit: " + unitName + "@"
                + candidates.get(0).getContact().getId());
        return candidates.get(0);
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
            Logger.d(this.getClass(), "#selectUnitBySubtask - Unit: " +
                    candidates.get(0).getCapability().getName() + "@" + candidates.get(0).getContact().getId());
            return candidates.get(0);
        }

        // 按照最近执行时间戳从低到高排序
        Collections.sort(candidates, new Comparator<AIGCUnit>() {
            @Override
            public int compare(AIGCUnit u1, AIGCUnit u2) {
                return (int)(u1.getLastRunningTimestamp() - u2.getLastRunningTimestamp());
            }
        });

        // 先进行一次选择，选择最久没有执行的
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
            // 所有单元都在运行
            Logger.d(this.getClass(), "#selectUnitBySubtask - Unit: " +
                    unit.getCapability().getName() + "@" + unit.getContact().getId());
            return unit;
        }

        // 返回最久的单元
        Logger.d(this.getClass(), "#selectUnitBySubtask - Unit: " +
                candidates.get(0).getCapability().getName() + "@" + candidates.get(0).getContact().getId());
        return candidates.get(0);
//        unit = candidates.get(Utils.randomInt(0, candidates.size() - 1));
//        return unit;
    }

    //-------- App Interface - Start --------

    public boolean fireEvent(AppEvent appEvent) {
        AIGCHook hook = this.pluginSystem.getAppEventHook();
        AIGCPluginContext context = new AIGCPluginContext(appEvent);
        hook.apply(context);

        return this.storage.writeAppEvent(appEvent);
    }

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

        // 生成10位ID
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
        ContactManager.getInstance().newContact(id, domain, name, user.toJSON(), device);
        return user;
    }

    public User modifyUser(String token, UserModification modification) {
        AuthToken authToken = this.getToken(token);
        if (null == authToken) {
            return null;
        }

        Contact contact = ContactManager.getInstance().getContact(authToken.getDomain(), authToken.getContactId());
        if (null == contact) {
            return null;
        }

        User user = new User(contact.getContext());
        if (null != modification.displayName) {
            user.setDisplayName(modification.displayName);
        }
        ContactManager.getInstance().updateContact(contact.getDomain().getName(), contact.getId(), contact.getName(),
                user.toJSON(), null);
        return user;
    }

    public User checkInUser(Contact contact, VerificationCode verificationCode) {
        // 查找用户
        ContactSearchResult searchResult = ContactManager.getInstance().searchWithContactName(
                contact.getDomain().getName(), verificationCode.phoneNumber);
        if (searchResult.getContactList().isEmpty()) {
            Logger.i(this.getClass(), "#updateUser - New user: " + contact.getId());

            // 新注册用户
            User user = new User(contact.getContext());
            user.setRegisterTime(System.currentTimeMillis());
            user.setName(verificationCode.phoneNumber);
            user.setDisplayName(verificationCode.phoneNumber);
            user.setPhoneNumber(verificationCode.dialCode + "-" + verificationCode.phoneNumber);

            ContactManager.getInstance().updateContact(contact.getDomain().getName(),
                    contact.getId(), verificationCode.phoneNumber, user.toJSON(),
                    contact.getDevice());

            // 更新个人知识记忆
            this.updatePersonalKnowledgeBase(ContactManager.getInstance().getAuthToken(contact.getDomain().getName(),
                    contact.getId()), user);

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

            if (contact.getId().longValue() != userContact.getId().longValue()) {
                // 临时联系人标记为作废
                ContactManager.getInstance().setContactMask(contact.getDomain().getName(), contact.getId(),
                        ContactMask.Deprecated);
                // 删除对应的知识库
                getKnowledgeFramework().deleteKnowledgeBase(contact.getId(), User.KnowledgeBaseName);
            }

            // 更新老用户的令牌
            final long tokenDuration = 5L * 365 * 24 * 60 * 60 * 1000;
            AuthToken authToken = new AuthToken(tokenCode, userContact.getDomain().getName(),
                    AuthConsts.DEFAULT_APP_KEY, userContact.getId(),
                    System.currentTimeMillis(), System.currentTimeMillis() + tokenDuration, false);
            AuthToken newToken = authService.updateAuthTokenCode(authToken);
            // 新令牌
            User user = new User(userContact.getContext());
            user.setAuthToken(newToken);

            ContactManager.getInstance().updateContact(userContact.getDomain().getName(),
                    userContact.getId(), verificationCode.phoneNumber, user.toJSON(),
                    contact.getDevice());
            return user;
        }
    }

    public User checkInUser(boolean register, String userName, String password, Contact contact) {
        ContactSearchResult searchResult = ContactManager.getInstance()
                .searchWithContactName(contact.getDomain().getName(), userName);
        if (register) {
            if (searchResult.getContactList().isEmpty()) {
                // 注册新用户
                Logger.i(this.getClass(), "#checkInUser - register new user: " + userName);

                // 新用户
                User user = new User(contact.getContext());
                user.setRegisterTime(System.currentTimeMillis());
                user.setName(userName);
                user.setDisplayName(TextUtils.extractEmailAccountName(userName));
                user.setEmail(userName);
                user.setPassword(password);

                ContactManager.getInstance().updateContact(contact.getDomain().getName(),
                        contact.getId(), userName, user.toJSON(),
                        contact.getDevice());

                // 更新个人知识记忆
                this.updatePersonalKnowledgeBase(ContactManager.getInstance().getAuthToken(contact.getDomain().getName(),
                        contact.getId()), user);

                return user;
            }
            else {
                // 用户已存在
                Logger.w(this.getClass(), "#checkInUser - The user already exists: " + userName);
                return null;
            }
        }
        else {
            if (searchResult.getContactList().isEmpty()) {
                // 用户不存在
                Logger.w(this.getClass(), "#checkInUser - The user is NOT exist.: " + userName);
                return null;
            }
            else {
                // 校验用户名和密码
                Logger.i(this.getClass(), "#checkInUser - User login: " + contact.getId());

                Contact userContact = searchResult.getContactList().get(0);
                User user = new User(userContact.getContext());
                if (user.getName().equals(userName) && user.getPassword().equals(password)) {
                    // 校验通过
                    AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
                    // 当前使用令牌码，登录的账号继承当前临时账号的令牌码
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

                    if (contact.getId().longValue() != userContact.getId().longValue()) {
                        // 临时联系人标记为作废
                        ContactManager.getInstance().setContactMask(contact.getDomain().getName(), contact.getId(),
                                ContactMask.Deprecated);
                        // 删除对应的知识库
                        getKnowledgeFramework().deleteKnowledgeBase(contact.getId(), User.KnowledgeBaseName);
                    }

                    // 更新老用户的令牌
                    final long tokenDuration = 5L * 365 * 24 * 60 * 60 * 1000;
                    AuthToken authToken = new AuthToken(tokenCode, userContact.getDomain().getName(),
                            AuthConsts.DEFAULT_APP_KEY, userContact.getId(),
                            System.currentTimeMillis(), System.currentTimeMillis() + tokenDuration, false);
                    AuthToken newToken = authService.updateAuthTokenCode(authToken);
                    // 新令牌
                    user.setAuthToken(newToken);

                    ContactManager.getInstance().updateContact(userContact.getDomain().getName(),
                            userContact.getId(), user.getName(), user.toJSON(),
                            contact.getDevice());

                    // 更新个人知识记忆
                    this.updatePersonalKnowledgeBase(ContactManager.getInstance().getAuthToken(userContact.getDomain().getName(),
                            userContact.getId()), user);

                    return user;
                }
                else {
                    Logger.d(this.getClass(), "#checkInUser - Incorrect password: " + contact.getId() + " - "
                            + userName + " - " + password);
                    return null;
                }
            }
        }
    }

    private void updatePersonalKnowledgeBase(AuthToken authToken, User user) {
        KnowledgeBase base = this.getKnowledgeFramework().getKnowledgeBase(user.getId(), User.KnowledgeBaseName);
        if (null == base) {
            this.getKnowledgeFramework().newKnowledgeBase(authToken.getCode(), User.KnowledgeBaseName,
                    User.KnowledgeBaseDisplayName, "Profile", KnowledgeScope.Private);

            base = this.getKnowledgeFramework().getKnowledgeBase(user.getId(), User.KnowledgeBaseName);
            if (null == base) {
                Logger.e(this.getClass(), "#updatePersonalKnowledgeBase - Failed: " + user.getId());
                return;
            }

            KnowledgeProfile profile = base.getProfile();
            Logger.d(this.getClass(), "#updatePersonalKnowledgeBase - New personal base: " + user.getId() + " - " +
                    profile.scope.name + "/" + profile.maxSize + "/" + profile.state);
        }

        final KnowledgeBase knowledgeBase = base;
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                if (Logger.isDebugLevel()) {
                    Logger.d(this.getClass(), "#updatePersonalKnowledgeBase - " + user.getId() +
                            " - " + knowledgeBase.getName());
                }

                StringBuilder markdown = new StringBuilder();
                markdown.append(user.markdown());
                Membership membership = ContactManager.getInstance().getMembershipSystem().getMembership(
                        authToken.getDomain(), user.getId(), Membership.STATE_NORMAL);
                markdown.append(ContentTools.makeMembership(user, membership));

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());

                List<KnowledgeArticle> articleList = knowledgeBase.getKnowledgeArticlesByTitle(User.KnowledgeTitle);
                if (articleList.isEmpty()) {
                    if (Logger.isDebugLevel()) {
                        Logger.d(this.getClass(), "#updatePersonalKnowledgeBase - create profile data: "
                                + authToken.getContactId());
                    }

                    KnowledgeArticle article = new KnowledgeArticle(authToken.getDomain(), authToken.getContactId(),
                            User.KnowledgeBaseName,
                            "Profile", User.KnowledgeTitle, markdown.toString(), user.getName(),
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH) + 1,
                            calendar.get(Calendar.DATE),
                            System.currentTimeMillis(), KnowledgeScope.Private);
                    // 添加
                    article = knowledgeBase.appendKnowledgeArticle(article);
                    // 激活
                    knowledgeBase.activateKnowledgeArticles(Collections.singletonList(article.getId()),
                            TextSplitter.None);
                }
                else {
                    if (Logger.isDebugLevel()) {
                        Logger.d(this.getClass(), "#updatePersonalKnowledgeBase - update profile data: "
                                + authToken.getContactId());
                    }

                    KnowledgeArticle article = articleList.get(0);
                    article.content = markdown.toString();
                    // 更新
                    knowledgeBase.updateKnowledgeArticle(article);
                    // 激活
                    knowledgeBase.activateKnowledgeArticles(Collections.singletonList(article.getId()),
                            TextSplitter.None);
                }
            }
        });
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
                contact.getName() + "-" + ContactMask.SignOut.mask, user.toJSON(),
                contact.getDevice());
        // 删除令牌
        AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
        authService.deleteToken(contact.getDomain().getName(), contact.getId());
        return user;
    }

    /**
     * 使用邀请码激活会员。
     *
     * @param token 令牌。
     * @param channel 渠道代码。
     * @param invitationCode 邀请码。
     * @return
     */
    public Membership activateMembership(AuthToken token, String channel, String invitationCode) {
        Contact contact = ContactManager.getInstance().getContact(token.getDomain(), token.getContactId());
        if (null == contact) {
            return null;
        }

        // 处理特殊码
        if (invitationCode.equals("941017")) {
            // 强制取消会员身份
            return this.cancelMembership(token);
        }

        // 校验邀请码
        MembershipSystem.InvitationCode mic = ContactManager.getInstance().getMembershipSystem().verifyInvitationCode(
                invitationCode);
        if (null == mic) {
            return null;
        }

        JSONObject context = contact.getContext();
        if (null == context) {
            context = new JSONObject();
        }
        context.put("channel", channel);
        Membership membership = ContactManager.getInstance().getMembershipSystem().activateMembership(token.getDomain(),
                token.getContactId(), "MindEcho", mic, context);
        if (null != membership) {
            // 绑定验证码
            ContactManager.getInstance().getMembershipSystem().bindInvitationCode(mic, token.getContactId());

            // 更新会员信息
            this.updatePersonalKnowledgeBase(token, new User(context));
        }
        return membership;
    }

    /**
     * 取消指定用户的会员。
     *
     * @param token 令牌。
     * @return
     */
    public Membership cancelMembership(AuthToken token) {
        Contact contact = ContactManager.getInstance().getContact(token.getDomain(), token.getContactId());
        if (null == contact) {
            return null;
        }

        Membership membership = ContactManager.getInstance().getMembershipSystem().cancelMembership(
                token.getDomain(), token.getContactId());
        if (null != membership) {
            // 更新会员信息
            this.updatePersonalKnowledgeBase(token, new User(contact.getContext()));
        }
        return membership;
    }

    public WordCloud createWordCloud(AuthToken authToken) {
        WordCloud wordCloud = new WordCloud();

        long end = System.currentTimeMillis();
        long start = end - (365L * 24 * 60 * 60 * 1000);
        List<AIGCChatHistory> chatHistories = this.storage.readHistoriesByContactId(
                authToken.getContactId(), authToken.getDomain(), start, end);

        TFIDFAnalyzer analyzer = new TFIDFAnalyzer(this.tokenizer);
        for (AIGCChatHistory history : chatHistories) {
            List<String> words = analyzer.analyzeOnlyWords(history.queryContent, 10);
            for (String word : words) {
                wordCloud.addWord(word.trim());
            }
            words = analyzer.analyzeOnlyWords(history.answerContent, 10);
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
        if (null == tokenCode) {
            return null;
        }

        AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
        if (null == authService) {
            return null;
        }

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
     * @param language
     * @return
     */
    public AIGCChannel createChannel(String token, String participant, String channelCode, Language language) {
        AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
        AuthToken authToken = authService.getToken(token);
        if (null == authToken) {
            return null;
        }

        return this.createChannel(authToken, participant, channelCode, language);
    }

    /**
     * 创建频道。
     *
     * @param authToken
     * @param participant
     * @param channelCode
     * @param language
     * @return
     */
    public AIGCChannel createChannel(AuthToken authToken, String participant, String channelCode, Language language) {
        AIGCChannel channel = new AIGCChannel(authToken, participant, channelCode, language);
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
            // TODO XJW
//            for (Queue<GenerateTextUnitMeta> queue : this.generateQueueMap.values()) {
//                Iterator<GenerateTextUnitMeta> iter = queue.iterator();
//                while (iter.hasNext()) {
//                    GenerateTextUnitMeta meta = iter.next();
//                    if (meta.channel.getCode().equals(channelCode)) {
//                        iter.remove();
//                        channel.setProcessing(false);
//                        hit = true;
//                        break;
//                    }
//                }
//                if (hit) {
//                    break;
//                }
//            }

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
                                List<GeneratingRecord> histories, int maxHistories, List<Attachment> attachments,
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

        final GenerateTextUnitMeta meta = new GenerateTextUnitMeta(this, unit, channel, content, option, categories,
                histories, attachments, listener);
        meta.setMaxHistories(maxHistories);
        meta.setRecordHistoryEnabled(recordable);
        meta.setNetworkingEnabled(networking);

//        synchronized (this.generateQueueMap) {
//            Queue<GenerateTextUnitMeta> queue = this.generateQueueMap.get(unit.getQueryKey());
//            if (null == queue) {
//                queue = new ConcurrentLinkedQueue<>();
//                this.generateQueueMap.put(unit.getQueryKey(), queue);
//            }
//
//            queue.offer(meta);
//        }

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                processGenerateTextMeta(meta);
            }
        });

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
        AIGCUnit unit = this.selectUnitByName(unitName);
        if (null == unit) {
            Logger.w(this.getClass(), "#syncGenerateText - Can NOT find unit: " + unitName);
            return null;
        }
        return this.syncGenerateText(unit, prompt, option, history, participantContact);
    }

    /**
     * 同步方式生成文本。
     *
     * @param authToken
     * @param unitName
     * @param prompt
     * @param option
     * @param history
     * @param participantContact
     * @return
     */
    public GeneratingRecord syncGenerateText(AuthToken authToken, String unitName, String prompt, GeneratingOption option,
                                            List<GeneratingRecord> history, Contact participantContact) {
        AIGCUnit unit = this.selectUnitByName(unitName, authToken.getContactId());
        if (null == unit) {
            Logger.w(this.getClass(), "#syncGenerateText - Can NOT find unit: " + unitName);
            return null;
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

        Packet request = null;
        ActionDialect dialect = null;
        long sn = Utils.generateSerialNumber();
        unit.setRunning(true);
        try {
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

            JSONObject data = new JSONObject();
            data.put("unit", unit.getCapability().getName());
            data.put("content", prompt);
            data.put("participant", participant.toCompactJSON());
            data.put("history", historyArray);
            data.put("option", (null == option) ? (new GeneratingOption()).toJSON() : option.toJSON());

            request = new Packet(AIGCAction.TextToText.name, data);
            dialect = this.cellet.transmit(unit.getContext(), request.toDialect(),
                    8 * 60 * 1000, sn);
            if (null == dialect) {
                Logger.w(AIGCService.class, "#syncGenerateText - transmit failed, sn:" + sn
                        + " - " + unit.getCapability().getName() + "@" + unit.getContact().getId());
                // 记录故障
                unit.markFailure(AIGCStateCode.UnitError.code, System.currentTimeMillis(), participant.getId());
                count.decrementAndGet();
                return null;
            }
        } finally {
            unit.setRunning(false);
        }

        Packet response = new Packet(dialect);
        int stateCode = Packet.extractCode(response);
        if (stateCode != AIGCStateCode.Ok.code) {
            Logger.e(AIGCService.class, "#syncGenerateText - failed, state code:" + stateCode
                    + " - " + unit.getCapability().getName() + "@" + unit.getContact().getId());
            count.decrementAndGet();
            return null;
        }
        JSONObject payload = Packet.extractDataPayload(response);

        String responseText = "";
        String thoughtText = "";
        try {
            responseText = payload.getString("response");
            responseText = responseText.trim();
            thoughtText = payload.getString("thought");
            thoughtText = thoughtText.trim();
        } catch (Exception e) {
            Logger.w(AIGCService.class, "#syncGenerateText - failed, sn:" + sn
                    + " - " + unit.getCapability().getName() + "@" + unit.getContact().getId());
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
                             List<GeneratingRecord> histories, int maxHistories, List<Attachment> attachments,
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

        final GenerateTextUnitMeta meta = new GenerateTextUnitMeta(this, unit, channel, prompt, option, categories,
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

//        synchronized (this.generateQueueMap) {
//            Queue<GenerateTextUnitMeta> queue = this.generateQueueMap.get(unit.getQueryKey());
//            if (null == queue) {
//                queue = new ConcurrentLinkedQueue<>();
//                this.generateQueueMap.put(unit.getQueryKey(), queue);
//            }
//
//            queue.offer(meta);
//        }

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                processGenerateTextMeta(meta);
            }
        });
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
     * @deprecated
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
            channel = this.createChannel(tokenCode, "User-" + channelCode, channelCode, Language.Chinese);
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

        final ConversationUnitMeta meta = new ConversationUnitMeta(unit, channel, content, parameter, listener);

//        synchronized (this.conversationQueueMap) {
//            Queue<ConversationUnitMeta> queue = this.conversationQueueMap.get(unit.getQueryKey());
//            if (null == queue) {
//                queue = new ConcurrentLinkedQueue<>();
//                this.conversationQueueMap.put(unit.getQueryKey(), queue);
//            }
//
//            queue.offer(meta);
//        }

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                processConversationMeta(meta);
            }
        });

        return meta.sn;
    }

    /**
     *
     * @param channelCode
     * @param sn
     * @return
     * @deprecated
     */
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

//            synchronized (this.conversationQueueMap) {
//                for (Queue<ConversationUnitMeta> queue : this.conversationQueueMap.values()) {
//                    for (ConversationUnitMeta meta : queue) {
//                        if (meta.sn == sn) {
//                            unitMeta = meta;
//                            break;
//                        }
//                    }
//
//                    if (null != unitMeta) {
//                        break;
//                    }
//                }
//            }

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

        final UnitMeta meta = new SummarizationUnitMeta(this, unit, modified, listener);

        Queue<UnitMeta> queue = null;
        synchronized (this.summarizationQueueMap) {
            queue = this.summarizationQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.summarizationQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            final Queue<UnitMeta> metaQueue = queue;
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processQueue(meta.unit, metaQueue);
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

//        unit = this.selectUnitBySubtask(AICapability.Multimodal.TextToImage);
//        if (null == unit) {
//            Logger.w(AIGCService.class, "No text to image unit setup in server");
//            channel.setProcessing(false);
//            return false;
//        }

        final UnitMeta meta = new TextToImageUnitMeta(this, unit, channel, text, listener);

        Queue<UnitMeta> queue = null;
        synchronized (this.textToImageQueueMap) {
            queue = this.textToImageQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.textToImageQueueMap.put(unit.getQueryKey(), queue);
            }
            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            final Queue<UnitMeta> metaQueue = queue;
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processQueue(meta.unit, metaQueue);
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

        // 临时使用 PSYCHOLOGY_UNIT
        AIGCUnit unit = this.selectUnitByName(ModelConfig.PSYCHOLOGY_UNIT);
        if (null == unit) {
            unit = this.selectUnitByName(ModelConfig.BAIZE_UNIT);
            if (null == unit) {
                Logger.e(this.getClass(), "#generateFile - No unit, token: " + channel.getAuthToken().getCode());
                return false;
            }
        }

        final UnitMeta meta = new TextToFileUnitMeta(this, unit, channel, text, attachment.queryFileLabels, listener);

        Queue<UnitMeta> queue = null;
        synchronized (this.textToFileQueueMap) {
            queue = this.textToFileQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.textToFileQueueMap.put(unit.getQueryKey(), queue);
            }
            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            final Queue<UnitMeta> metaQueue = queue;
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processQueue(meta.unit, metaQueue);
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

        final UnitMeta meta = new ExtractKeywordsUnitMeta(this, unit, text, listener);

        Queue<UnitMeta> queue = null;
        synchronized (this.extractKeywordsQueueMap) {
            queue = this.extractKeywordsQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.extractKeywordsQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            final Queue<UnitMeta> metaQueue = queue;
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processQueue(meta.unit, metaQueue);
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

        final UnitMeta meta = new SemanticSearchUnitMeta(this, unit, query, listener);

        Queue<UnitMeta> queue = null;
        synchronized (this.semanticSearchQueueMap) {
            queue = this.semanticSearchQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.semanticSearchQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            final Queue<UnitMeta> metaQueue = queue;
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processQueue(meta.unit, metaQueue);
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

        final UnitMeta meta = new RetrieveReRankUnitMeta(this, unit, queries, listener);

        Queue<UnitMeta> queue = null;
        synchronized (this.retrieveReRankQueueMap) {
            queue = this.retrieveReRankQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.retrieveReRankQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            final Queue<UnitMeta> metaQueue = queue;
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processQueue(meta.unit, metaQueue);
                }
            });
        }

        return true;
    }

    public List<RetrieveReRankResult> syncRetrieveReRank(List<FileLabel> fileLabels, String query) {
        AIGCUnit unit = this.selectUnitBySubtask(AICapability.NaturalLanguageProcessing.RetrieveReRank);
        if (null == unit) {
            Logger.w(this.getClass(), "#syncRetrieveReRank - No retrieve re-rank unit setup in server");
            return null;
        }

        final List<RetrieveReRankResult> result = new ArrayList<>();

        UnitMeta meta = new RetrieveReRankUnitMeta(this, unit, fileLabels, query, new RetrieveReRankListener() {
            @Override
            public void onCompleted(List<RetrieveReRankResult> retrieveReRankResults) {
                result.addAll(retrieveReRankResults);
                synchronized (result) {
                    result.notify();
                }
            }

            @Override
            public void onFailed(List<String> queries, AIGCStateCode stateCode) {
                synchronized (result) {
                    result.notify();
                }
            }
        });

        Queue<UnitMeta> queue = null;
        synchronized (this.retrieveReRankQueueMap) {
            queue = this.retrieveReRankQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.retrieveReRankQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            final Queue<UnitMeta> metaQueue = queue;
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processQueue(meta.unit, metaQueue);
                }
            });
        }

        synchronized (result) {
            try {
                result.wait(5 * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

//        try {
//            Thread.sleep(Utils.randomInt(10 * 1000, 30 * 1000));
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        JSONObject json = new JSONObject();
//        json.put("query", query);
//        JSONArray array = new JSONArray();
//        JSONObject answer = new JSONObject();
//        answer.put("id", 1);
//        answer.put("content", "这是分析" + fileLabels.get(0).getFileName() + "文件对于\"" + query + "\"的提问的Re-rank测试。");
//        answer.put("score", 0.8);
//        array.put(answer);
//        json.put("list", array);
//        result.add(new RetrieveReRankResult(json));

        return result;
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
     * @param maxIndicators
     * @param adjust
     * @param listener
     * @return
     */
    public PaintingReport generatePaintingReport(String token, Attribute attribute, String fileCode,
                                                 Theme theme, int maxIndicators, boolean adjust,
                                                 PaintingReportListener listener) {
        if (!this.isStarted()) {
            Logger.w(this.getClass(), "#generatePaintingReport - The service has NOT started");
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
            Logger.d(this.getClass(), "#generatePaintingReport - max indicators: " + maxIndicators +
                    ", file: " + fileCode);
        }

        FileLabel fileLabel = new FileLabel(fileLabelJson);

        AIGCChannel channel = this.getChannelByToken(token);
        if (null == channel) {
            channel = this.createChannel(authToken, "Baize", Utils.randomString(16),
                    attribute.language);
        }

        // 生成报告
        PaintingReport report = PsychologyScene.getInstance().generatePsychologyReport(channel,
                attribute, fileLabel, theme, maxIndicators, adjust, 0, listener);

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

        return PsychologyScene.getInstance().generateScaleReport(channel, scale, channel.getLanguage(), listener);
    }

    /**
     * 生成心理学量表测验报告。
     *
     * @param token
     * @param scaleSn
     * @param language
     * @param listener
     * @return
     */
    public ScaleReport generateScaleReport(String token, long scaleSn, Language language, ScaleReportListener listener) {
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
                channel = this.createChannel(authToken, "Baize", Utils.randomString(16), language);
            }

            ScaleReport report = PsychologyScene.getInstance().generateScaleReport(channel, scale, language, listener);
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

        final UnitMeta meta = new SpeechRecognitionUnitMeta(this, unit, fileLabel, listener);

        Queue<UnitMeta> queue = null;
        synchronized (this.speechQueueMap) {
            queue = this.speechQueueMap.get(unit.getQueryKey());
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                this.speechQueueMap.put(unit.getQueryKey(), queue);
            }

            queue.offer(meta);
        }

        if (!unit.isRunning()) {
            final Queue<UnitMeta> metaQueue = queue;
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processQueue(meta.unit, metaQueue);
                }
            });
        }

        return true;
    }

    /**
     * 说话者分割与分析。
     *
     * @param authToken
     * @param fileLabel
     * @param preprocess
     * @param storage
     * @param jumpToFirst
     * @param listener
     * @return
     */
    public boolean performSpeakerDiarization(AuthToken authToken, FileLabel fileLabel, boolean preprocess,
                                             boolean storage, boolean jumpToFirst,
                                             VoiceDiarizationListener listener) {
//        AbstractModule fileStorage = this.getKernel().getModule("FileStorage");
//        if (null == fileStorage) {
//            Logger.e(this.getClass(), "#performSpeakerDiarization - File storage service is not ready");
//            return false;
//        }

        // 查找有该能力的单元
        AIGCUnit unit = this.selectUnitBySubtask(AICapability.AudioProcessing.SpeakerDiarization);
        if (null == unit) {
            Logger.w(this.getClass(), "#performSpeakerDiarization - No task unit setup in server");
            return false;
        }

        final AudioUnitMeta meta = new AudioUnitMeta(this, unit, authToken, AIGCAction.SpeakerDiarization,
                fileLabel, preprocess, storage);
        meta.voiceDiarizationListener = listener;

        LinkedList<UnitMeta> queue = this.audioQueueMap.computeIfAbsent(unit.getQueryKey(), k -> new LinkedList<>());
        synchronized (queue) {
            if (jumpToFirst) {
                queue.addFirst(meta);
            }
            else {
                queue.offer(meta);
            }
        }

        if (!unit.isRunning()) {
            final Queue<UnitMeta> metaQueue = queue;
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    processQueue(meta.unit, metaQueue);
                }
            });
        }

        return true;
    }

    /**
     * 说话者分割与分析。
     *
     * @param authToken
     * @param fileCode
     * @param preprocess
     * @param storage
     * @param listener
     * @return
     */
    public boolean performSpeakerDiarization(AuthToken authToken, String fileCode, boolean preprocess,
                                             boolean storage, VoiceDiarizationListener listener) {
//        AbstractModule fileStorage = this.getKernel().getModule("FileStorage");
//        if (null == fileStorage) {
//            Logger.e(this.getClass(), "#performSpeakerDiarization - File storage service is not ready");
//            return false;
//        }

        FileLabel fileLabel = this.getFile(authToken.getDomain(), fileCode);
        if (null == fileLabel) {
            Logger.e(this.getClass(), "#performSpeakerDiarization - Get file failed: " + fileCode);
            return false;
        }

        return this.performSpeakerDiarization(authToken, fileLabel, preprocess, storage, false, listener);
    }

    public List<VoiceDiarization> getVoiceDiarizations(AuthToken authToken) {
        List<VoiceDiarization> result = this.storage.readVoiceDiarizations(authToken.getContactId());
        for (VoiceDiarization voiceDiarization : result) {
            voiceDiarization.file = this.getFile(authToken.getDomain(), voiceDiarization.fileCode);
        }
        return result;
    }

    public VoiceDiarization deleteVoiceDiarization(AuthToken authToken, String fileCode) {
        VoiceDiarization voiceDiarization = this.storage.readVoiceDiarization(fileCode);
        voiceDiarization.file = this.getFile(authToken.getDomain(), voiceDiarization.fileCode);
        this.storage.deleteVoiceDiarization(fileCode);
        return voiceDiarization;
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
                    Logger.w(AIGCService.class, "#speechEmotionRecognition - No support file: " +
                            fileLabel.getFileType().getPreferredExtension());
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

    /**
     * 分析语音流。
     *
     * @param authToken
     * @param fileCode
     * @param streamName
     * @param index
     * @param listener
     * @return
     */
    public boolean analyseVoiceStream(AuthToken authToken, String fileCode, String streamName, int index,
                                      VoiceStreamAnalysisListener listener) {
        VoiceStreamSink streamSink = new VoiceStreamSink(streamName, index);

//        {
//            VoiceDiarization voiceDiarization = new VoiceDiarization(Utils.generateSerialNumber(),
//                    System.currentTimeMillis(), authToken.getContactId(),
//                    "这是标题", "", fileCode, 6.1, Utils.randomInt(1000, 5000));
//            (new Thread() {
//                @Override
//                public void run() {
//                    try {
//                        Thread.sleep(Utils.randomInt(5000, 9000));
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//
//                    FileLabel fileLabel = getFile(authToken.getDomain(), fileCode);
//                    streamSink.setDiarization(voiceDiarization);
//                    streamSink.setFileLabel(fileLabel);
//
//                    listener.onCompleted(fileLabel, streamSink);
//                }
//            }).start();
//            return true;
//        }

        boolean success = this.performSpeakerDiarization(authToken, fileCode, false, false,
                new VoiceDiarizationListener() {
            @Override
            public void onCompleted(FileLabel source, VoiceDiarization diarization) {
                streamSink.setDiarization(diarization);
                streamSink.setFileLabel(source);

                if (Logger.isDebugLevel()) {
                    Logger.d(this.getClass(), "#onCompleted - stream sink completed: " + fileCode);
                }

                listener.onCompleted(source, streamSink);

                for (VoiceTrack track : diarization.tracks) {
                    List<String> words = track.recognition.words;
                    boolean japanese = false;
                    for (String word : words) {
                        if (TextUtils.isJapanese(word)) {
                            japanese = true;
                            break;
                        }
                    }
                    if (japanese) {
                        track.recognition = new SpeechRecognitionInfo(track.recognition, "…");
                    }
                }

                // 记录流
                CounselingManager.getInstance().record(authToken, streamSink);
            }

            @Override
            public void onFailed(FileLabel source, AIGCStateCode stateCode) {
                synchronized (streamSink) {
                    streamSink.notify();
                }

                listener.onFailed(source, stateCode);
            }
        });

        return success;
    }

    /**
     * 面部表情识别。
     *
     * @param token
     * @param fileCode
     * @param visualize
     * @param listener
     * @return
     */
    public boolean facialExpressionRecognition(AuthToken token, String fileCode, boolean visualize,
                                               FacialExpressionRecognitionListener listener) {
        final FileLabel fileLabel = this.getFile(token.getDomain(), fileCode);
        if (null == fileLabel) {
            Logger.w(this.getClass(), "#facialExpressionRecognition - Can NOT find file: " + fileCode);
            return false;
        }

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                AIGCUnit unit = selectUnitByName(ModelConfig.FACIAL_EXPRESSION_UNIT);
                if (null == unit) {
                    Logger.w(AIGCService.class, "#facialExpressionRecognition - No unit");
                    listener.onFailed(fileLabel, AIGCStateCode.UnitNoReady);
                    return;
                }

                // 判断文件类型
                if (fileLabel.getFileType() != FileType.JPEG &&
                        fileLabel.getFileType() != FileType.PNG &&
                        fileLabel.getFileType() != FileType.BMP) {
                    Logger.w(AIGCService.class, "#facialExpressionRecognition - No support file: " +
                            fileLabel.getFileType().getPreferredExtension());
                    listener.onFailed(fileLabel, AIGCStateCode.FileError);
                    return;
                }

                JSONObject payload = new JSONObject();
                payload.put("fileLabel", fileLabel.toJSON());
                payload.put("visualize", visualize);
                Packet request = new Packet(AIGCAction.FacialExpressionRecognition.name, payload);
                ActionDialect dialect = cellet.transmit(unit.getContext(), request.toDialect(), 2 * 60 * 1000);
                if (null == dialect) {
                    Logger.w(AIGCService.class, "#facialExpressionRecognition - Unit error");
                    // 回调错误
                    listener.onFailed(fileLabel, AIGCStateCode.UnitError);
                    return;
                }

                Packet response = new Packet(dialect);
                JSONObject data = Packet.extractDataPayload(response);
                if (!data.has("result")) {
                    Logger.w(AIGCService.class, "#facialExpressionRecognition - Unit process failed");
                    // 回调错误
                    listener.onFailed(fileLabel, AIGCStateCode.Failure);
                    return;
                }

                FacialExpressionResult result = new FacialExpressionResult(data.getJSONObject("result"));
                // 回调结束
                listener.onCompleted(fileLabel, result);
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

    public FileLabel saveFile(AuthToken authToken, String fileCode, File file, String filename, boolean deleteAfterSave) {
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
            if (deleteAfterSave && file.exists()) {
                try {
                    file.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public FileLabel deleteFile(String domain, String fileCode) {
        AbstractModule fileStorage = this.getKernel().getModule("FileStorage");
        if (null == fileStorage) {
            Logger.e(this.getClass(), "#deleteFile - File storage service is not ready");
            return null;
        }

        DeleteFile deleteFile = new DeleteFile(domain, fileCode);
        try {
            JSONObject fileLabelJson = fileStorage.notify(deleteFile);
            return new FileLabel(fileLabelJson);
        } catch (Exception e) {
            Logger.e(this.getClass(), "#deleteFile - Delete file failed", e);
            return null;
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
     * 识别上下文数据。
     *
     * @param text
     * @param authToken
     * @return
     */
    public ComplexContext recognizeContext(String text, AuthToken authToken) {
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
            // TODO 2025-7-24 需要重写该工作流
//            Stage stage = Explorer.getInstance().perform(authToken, content);
//            if (stage.isFlowable()) {
//                result = new ComplexContext(false);
//                result.stage = stage;
//            }

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

    private void processGenerateTextMeta(GenerateTextUnitMeta meta) {
//        Queue<GenerateTextUnitMeta> queue = this.generateQueueMap.get(queryKey);
//        if (null == queue) {
//            Logger.w(AIGCService.class, "#processGenerateTextQueue - Not found unit: " + queryKey);
//            AIGCUnit unit = this.unitMap.get(queryKey);
//            if (null != unit) {
//                unit.setRunning(false);
//            }
//            return;
//        }

        AIGCUnit unit = meta.unit;

        int countdown = 50;
        while (unit.isRunning()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            --countdown;
            if (countdown <= 0) {
                break;
            }
        }

        unit.setRunning(true);

        AtomicInteger count = this.generateTextUnitCountMap.get(unit.getCapability().getName());
        if (null == count) {
            count = new AtomicInteger(1);
            this.generateTextUnitCountMap.put(unit.getCapability().getName(), count);
        }
        else {
            count.incrementAndGet();
        }

        // 执行处理
        try {
            meta.process();
        } catch (Exception e) {
            Logger.e(this.getClass(), "#processGenerateTextMeta - meta process", e);
        }

        count.decrementAndGet();

        unit.setRunning(false);
    }

    private void processConversationMeta(ConversationUnitMeta meta) {
//        Queue<ConversationUnitMeta> queue = this.conversationQueueMap.get(queryKey);
//        if (null == queue) {
//            Logger.w(AIGCService.class, "Not found unit: " + queryKey);
//            return;
//        }

        meta.unit.setRunning(true);

        try {
            meta.process();
        } catch (Exception e) {
            Logger.e(this.getClass(), "#processConversationMeta - meta process", e);
        }

        meta.unit.setRunning(false);
    }

    private void processQueue(AIGCUnit unit, Queue<UnitMeta> queue) {
        unit.setRunning(true);

        UnitMeta meta = null;
        synchronized (queue) {
            meta = queue.poll();
        }
        while (null != meta) {
            // 执行处理
            try {
                meta.process();
            } catch (Exception e) {
                Logger.e(this.getClass(), "#processQueue - meta process error", e);
            }

            synchronized (queue) {
                meta = queue.poll();
            }
        }

        unit.setRunning(false);
    }

    private boolean checkParticipantName(String name) {
        if (name.equalsIgnoreCase("AIGC") || name.equalsIgnoreCase("Cube") ||
            name.equalsIgnoreCase("Baize") || name.equalsIgnoreCase("白泽")) {
            return false;
        }
        else {
            return true;
        }
    }

    /**
     * @deprecated
     */
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
            super(AIGCService.this, unit, channel, content, parameter.toGenerativeOption(), parameter.categories,
                    parameter.records, null, null);
            this.listener = this.generateListener;
            this.maxHistories = parameter.histories;
            this.recordHistoryEnabled = parameter.recordable;
            this.networkingEnabled = parameter.networking;
            this.parameter = parameter;
            this.conversationListener = listener;
        }
    }
}
