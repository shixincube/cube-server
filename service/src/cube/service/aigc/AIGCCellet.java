/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.spi.ActionRouter;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.state.AIGCStateCode;
import cube.core.AbstractCellet;
import cube.core.Kernel;
import cube.service.Director;
import cube.service.aigc.event.EventCenter;
import cube.service.aigc.spi.AIGCHostImpl;
import cube.service.aigc.spi.ActionRunner;
import cube.service.aigc.spi.ModuleRegistry;
import cube.service.aigc.task.*;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * AIGC 服务单元。
 */
public class AIGCCellet extends AbstractCellet {

    private AIGCService service;

    private ConcurrentLinkedQueue<Responder> responderList;

    /**
     * AIGC 业务模块注册表。
     *
     * <p>在 {@link #install()} 中创建，<b>装载</b>则由 {@code AIGCService}
     * 的引导线程经 {@link #loadModules()} 触发（延后原因是模块 setup
     * 可能读取宿主存储配置）。无论是否装载完毕，它都早于任何一次
     * {@link #onListened} 派发，因此派发路径上不需要判空。</p>
     *
     * <p>模块清单由 {@code config/aigc-modules.properties} 决定；
     * 该文件未列出任何模块时，注册表为空，派发成本为一次 volatile 读。</p>
     */
    private ModuleRegistry moduleRegistry;

    /**
     * 宿主能力实现，向业务模块提供受控的服务访问能力。
     *
     * <p>与 {@link #moduleRegistry} 同在 {@code install()} 阶段创建，
     * 早于任何一次动作派发，因此派发时该字段必然已就绪。</p>
     */
    private AIGCHostImpl aigcHost;

    /**
     * 业务模块降级期间是否已发出过运行期告警。
     *
     * <p>非volatile：仅在派发线程中读写，无跨线程可见性需求。
     * 降级状态本身由 {@code AIGCService} 的 volatile 字段承载。</p>
     */
    private boolean degradationWarned = false;

    public AIGCCellet() {
        super(AIGCService.NAME);
        this.responderList = new ConcurrentLinkedQueue<>();
    }

    @Override
    public boolean install() {
        this.service = new AIGCService(this);

        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.installModule(AIGCService.NAME, this.service);

        // 业务模块注册表与宿主能力实现在此构造（registry → host → setHost）。
        //
        // ⚠️ 此处【不】装载模块（load）：装载需要宿主存储就绪，而 install() 早于
        // 引导线程中的存储创建。装载由 AIGCService 在存储就绪后调用 loadModules() 触发。
        //
        // 即便装载晚于 install，也仍早于任何一次 onListened（内核启动后才收报文），
        // 故路由表在首个请求到达前已权威。
        this.moduleRegistry = new ModuleRegistry(new ActionRouter());
        this.aigcHost = new AIGCHostImpl(this.service, this.moduleRegistry);
        this.moduleRegistry.setHost(this.aigcHost);

        return true;
    }

    /**
     * 装载已注册的业务模块。
     *
     * <p>由宿主在<b>自身存储就绪之后</b>、于引导线程中调用。延后的原因：
     * 模块的 {@code setup} 可能读取宿主存储配置，宿主存储未就绪时装载会失败。
     * 装载亦承担业务场景的装配——由模块 setup 注入场景所需的宿主能力与存储。</p>
     *
     * <p>即便装载晚于 install，也仍早于任何一次 onListened（内核启动后才收报文），
     * 故路由表在首个请求到达前已权威。</p>
     *
     * <p><b>⚠️ 不可重复调用</b>：{@link ModuleRegistry#load()} 自身没有重复装载保护，
     * 二次调用会再次实例化并再次 {@code setup} 各模块，模块必须能承受重复初始化。
     * 宿主因此只在此处调用一次。</p>
     *
     * @return 成功装载的模块数。
     */
    public int loadModules() {
        ModuleRegistry registry = this.moduleRegistry;

        if (null == registry) {
            Logger.w(this.getClass(), "#loadModules - Module registry is NOT available");
            return 0;
        }

        return registry.load();
    }

    @Override
    public void uninstall() {
        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.uninstallModule(AIGCService.NAME);

        for (Responder responder : this.responderList) {
            responder.finish();
        }
        this.responderList.clear();

        // 逆序释放：先停模块（释放其资源），再关延迟执行器（停掉仍待执行的模块任务）
        if (null != this.moduleRegistry) {
            this.moduleRegistry.teardownAll();
        }

        if (null != this.aigcHost) {
            this.aigcHost.shutdown();
        }
    }

    /**
     * 获取业务模块注册表。
     *
     * @return 返回注册表；未装载时返回 <code>null</code>。
     */
    public ModuleRegistry getModuleRegistry() {
        return this.moduleRegistry;
    }

    /**
     * 获取宿主能力实现。
     *
     * @return 返回宿主能力实现；未装载时返回 <code>null</code>。
     */
    public AIGCHostImpl getAIGCHost() {
        return this.aigcHost;
    }

    /**
     * 判断某动作是否「已由业务模块声明、但当前未绑定」。
     *
     * <p>用于识别「所属模块未装载或装载失败」——此时动作既不在模块中、
     * 也不在下方既有分支里，若不作处理，请求将无人应答而悬挂。</p>
     *
     * @param action 动作名。
     * @return 属于「已声明但未绑定」时返回 <code>true</code>。
     */
    private boolean isDeclaredButUnbound(String action) {
        ModuleRegistry registry = this.moduleRegistry;

        return null != registry
                && registry.getRouter().isLoaded()
                && registry.getRouter().isDeclared(action)
                && null == registry.getRouter().lookup(action);
    }

    /**
     * 构造「业务模块未就绪」的应答。
     *
     * <p>应答须带回 {@code _performer}，dispatcher 依此把结果回送给原调用方，
     * 否则调用方收不到任何回应。该字段的复制逻辑与
     * {@code ServiceTask#makeDispatcherResponse} 一致，此处复刻是因为
     * 本类并非 {@code ServiceTask} 的子类，无法复用其受保护方法。</p>
     *
     * @param dialect 请求方言。
     * @return 返回应答方言。
     */
    private ActionDialect makeModuleNotLoadedResponse(ActionDialect dialect) {
        Packet request = new Packet(dialect);

        // 载荷字段名与 ServiceTask#makePacketPayload 一致：code + data
        JSONObject payload = new JSONObject();
        payload.put("code", AIGCStateCode.ModuleNotLoaded.code);
        payload.put("data", new JSONObject());

        Packet response = new Packet(request.sn, request.name, payload);
        ActionDialect responseDialect = response.toDialect();
        Director.copyPerformer(dialect, responseDialect);
        return responseDialect;
    }

    /**
     * 校验已装载业务模块声明的单元能力是否均可用。
     *
     * <p><b>调用时机</b>：必须在宿主就绪之后——单元由 Relay 上报产生，
     * {@code install()} 阶段单元表必然为空，此刻校验只会得到全量误报。
     * 因此本方法不在装载路径上自动执行，由宿主在服务就绪后显式调用一次。</p>
     *
     * <p>校验失败不阻止模块工作，只记录明确的 WARN：能力是否应当存在
     * 取决于部署形态，宿主不预置业务能力名，无从判断「缺失」是配置错误还是部署预期。</p>
     *
     * @return 全部声明能力均可用时返回 <code>true</code>。
     */
    public boolean verifyModuleCapabilities() {
        ModuleRegistry registry = this.moduleRegistry;

        if (null == registry) {
            return true;
        }

        return registry.verifyCapabilities();
    }

    public AIGCService getService() {
        return this.service;
    }

    public ActionDialect transmit(TalkContext talkContext, ActionDialect dialect) {
        return this.transmit(talkContext, dialect, 3 * 60 * 1000);
    }

    public ActionDialect transmit(TalkContext talkContext, ActionDialect dialect, long timeout) {
        return this.transmit(talkContext, dialect, timeout, Utils.generateSerialNumber());
    }

    public ActionDialect transmit(TalkContext talkContext, ActionDialect dialect, long timeout, long sn) {
        Responder responder = new Responder(sn, dialect);
        this.responderList.add(responder);

        if (!this.speak(talkContext, dialect)) {
            Logger.w(AIGCCellet.class, "Speak session error: " + talkContext.getSessionHost());
            this.responderList.remove(responder);
            return null;
        }

        ActionDialect response = responder.waitingFor(timeout);
        if (null == response) {
            Logger.w(AIGCCellet.class, "Response is null: " + talkContext.getSessionHost());
            this.responderList.remove(responder);
            return null;
        }

        return response;
    }

    public void interrupt(long sn) {
        Responder responder = null;
        for (Responder r : this.responderList) {
            if (r.getSN() == sn) {
                responder = r;
                break;
            }
        }

        if (null == responder) {
            return;
        }

        Logger.d(AIGCCellet.class, "Response (" + sn + ") interrupt");
        this.responderList.remove(responder);
        responder.notifyResponse(new ActionDialect("interrupt"));
    }

    public boolean isInterruption(ActionDialect actionDialect) {
        return actionDialect.getName().equalsIgnoreCase("interrupt");
    }

    /**
     * 尝试把请求派发给已注册的业务模块。
     *
     * <p>模块清单未列出任何模块时，本方法在「是否已绑定」检查后立即返回
     * <code>false</code>，因此既不会建立应答计时记录，也不会向线程池提交任务，
     * 对既有动作分支与未匹配请求不产生任何可观测差异。</p>
     *
     * @param talkContext 会话上下文。
     * @param primitive 原始数据。
     * @param dialect 动作方言，复用已构造的实例。
     * @return 已由业务模块接管并提交任务时返回 <code>true</code>。
     */
    private boolean dispatchToModule(TalkContext talkContext, Primitive primitive, ActionDialect dialect) {
        ModuleRegistry registry = this.moduleRegistry;

        if (null == registry || !registry.getRouter().isLoaded()) {
            return false;
        }

        ActionRouter.Bound bound = registry.getRouter().lookup(dialect.getName());
        if (null == bound) {
            return false;
        }

        // 仅命中业务模块时才创建应答计时记录，与既有动作分支的语义一致
        this.execute(new ActionRunner(this, talkContext, primitive, this.markResponseTime(dialect.getName()),
                registry.getHost(), bound.getBinding(), bound.getOwner()));
        return true;
    }

    /**
     * 业务模块未就绪时，首次遇到相关动作记一条 ERROR。
     *
     * <p>目的是让「降级」在运行期也可被察觉：装载阶段的 ERROR 说明原因，
     * 而这条说明「降级期间确实有请求到达」——若运维在服务运行一段时间后
     * 才从客户端侧得知功能不可用，中间没有任何线索。</p>
     *
     * <p><b>只记一次</b>：降级期间每次请求都打日志会迅速刷满日志文件，
     * 而「发生过一次」这一事实已足够定位。</p>
     *
     * @param action 动作名。
     */
    private void warnDegradedOnce(String action) {
        if (this.degradationWarned) {
            return;
        }

        AIGCService service = this.service;
        if (null == service || !service.isModuleDegraded()) {
            return;
        }

        this.degradationWarned = true;

        Logger.e(this.getClass(), "#onListened - Business modules are DEGRADED; action \""
                + action + "\" and the rest of the business domain are NOT available."
                + " This message is logged only once per cellet lifetime.");
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        ActionDialect dialect = new ActionDialect(primitive);
        String action = dialect.getName();

        // 业务模块派发：未命中时立即返回 false，继续走下方的动作分支，行为不变。
        // 应答阻塞分支（Responder.NotifierKey）优先级最高，此处显式排除，确保它不会被业务模块劫持。
        if (!dialect.containsParam(Responder.NotifierKey) && this.dispatchToModule(talkContext, primitive, dialect)) {
            return;
        }

        // 该动作已由业务模块声明、但未绑定 —— 说明所属模块未装载或装载失败。
        // 此处必须明确应答：若继续往下走，该动作既不在模块中、也不在下方分支里，
        // 请求将无人应答而悬挂。此处返回 ModuleNotLoaded，
        // 客户端得以区分「业务域不可用」与「服务端故障」。
        if (!dialect.containsParam(Responder.NotifierKey)
                && this.isDeclaredButUnbound(action)) {
            Logger.w(this.getClass(), "#onListened - Action \"" + action
                    + "\" is declared by an AIGC module which is NOT loaded");
            this.speak(talkContext, this.makeModuleNotLoadedResponse(dialect));
            return;
        }

        // 降级状态下首次遇到「本应由业务模块处理」的动作时记一次 ERROR。
        // 不记WARN 的理由：降级本身已在装载阶段记过 ERROR，这里若每次都打
        // WARN 会把日志刷满，而「降级期间收到业务请求」这一事实只需告知一次。
        // 抑制标记使得降级持续期间不重复输出。
        this.warnDegradedOnce(action);

        if (dialect.containsParam(Responder.NotifierKey)) {
            // 应答阻塞访问
            for (Responder responder : this.responderList) {
                if (responder.isResponse(dialect)) {
                    responder.notifyResponse(dialect);
                    this.responderList.remove(responder);
                    break;
                }
            }
        }
        else if (AIGCAction.CheckToken.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new CheckTokenTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.AppGetOrCreateUser.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new AppGetOrCreateUserTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.AppModifyUser.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new AppModifyUserTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.AppCheckInUser.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new AppCheckInUserTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.AppInjectOrGetToken.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new AppInjectOrGetTokenTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.AppGetUserProfile.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new AppGetUserProfileTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.AppActivateMembership.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new AppActivateMembershipTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GetWordCloud.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GetWordCloudTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.AppSignOutUser.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new AppSignOutUserTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.AppVersion.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new AppVersionTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.AppASCIIArt.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new AppASCIIArtTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.Summarization.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new SummarizationTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.SemanticSearch.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new SemanticSearchTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.Segmentation.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new SegmentationTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.Chat.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new ChatTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.Multimodal.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new MultimodalTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
//        else if (AIGCAction.QueryMultimodal.name.equals(action)) {
//            // 来自 Dispatcher 的请求
//            this.execute(new QueryMultimodalTask(this, talkContext, primitive,
//                    this.markResponseTime(action)));
//        }
        else if (AIGCAction.GetSearchResults.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GetSearchResultsTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GetContextInference.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GetContextInferenceTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.KeepAliveChannel.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new KeepAliveChannelTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GetChannelInfo.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GetChannelTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.RequestChannel.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new RequestChannelTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.StopChannel.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new StopChannelTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.Evaluate.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new EvaluateTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.AddAppEvent.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new AddAppEventTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.QueryAppEvent.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new QueryAppEventTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.QueryUsages.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new QueryUsageTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.QueryChatHistory.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new QueryChatHistoryTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.AutomaticSpeechRecognition.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new AutomaticSpeechRecognitionTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.FacialExpressionRecognition.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new FacialExpressionRecognitionTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GetSpeechDiarization.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GetSpeechDiarizationTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.SpeechDiarization.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new SpeechDiarizationTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.ListSpeechDiarizations.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new ListSpeechDiarizationsTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.DeleteSpeechDiarization.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new DeleteSpeechDiarizationTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.SpeechEmotionRecognition.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new SpeechEmotionRecognitionTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GetEmotionRecords.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GetEmotionRecordsTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GetQueueCount.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GetQueueCountTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.TextToFile.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new TextToFileTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GetConfig.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GetConfigTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GenerateKnowledge.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GenerateKnowledgeTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GetKnowledgeProfile.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GetKnowledgeProfileTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.UpdateKnowledgeProfile.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new UpdateKnowledgeProfileTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GetKnowledgeQAProgress.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GetKnowledgeQAProgressTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.PerformKnowledgeQA.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new PerformKnowledgeQATask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GetKnowledgeFramework.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GetKnowledgeFrameworkTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.NewKnowledgeBase.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new NewKnowledgeBaseTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.DeleteKnowledgeBase.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new DeleteKnowledgeBaseTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.UpdateKnowledgeBase.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new UpdateKnowledgeBaseTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.ListKnowledgeDocs.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new ListKnowledgeDocsTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.ImportKnowledgeDoc.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new ImportKnowledgeDocTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.RemoveKnowledgeDoc.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new RemoveKnowledgeDocTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GetKnowledgeSegments.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GetKnowledgeSegmentsTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GetResetKnowledgeProgress.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GetResetKnowledgeProgressTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.ResetKnowledgeStore.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new ResetKnowledgeStoreTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GetBackupKnowledgeStores.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GetKnowledgeBackupTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GetKnowledgeProgress.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GetKnowledgeProgressTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.ListKnowledgeArticles.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new ListKnowledgeArticlesTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.ActivateKnowledgeArticle.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new ActivateKnowledgeArticleTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.DeactivateKnowledgeArticle.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new DeactivateKnowledgeArticleTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.AppendKnowledgeArticle.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new AppendKnowledgeArticleTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.RemoveKnowledgeArticle.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new RemoveKnowledgeArticleTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.UpdateKnowledgeArticle.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new UpdateKnowledgeArticleTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.QueryAllArticleCategories.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new QueryAllArticleCategoriesTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.ChartData.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new ChartDataTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GetPrompts.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GetPromptsTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.SetPrompts.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new SetPromptsTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.SubmitEvent.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new SubmitEventTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.Event.name.equalsIgnoreCase(action)) {
            // 来自 Unit 的请求
            this.execute(new Runnable() {
                @Override
                public void run() {
                    EventCenter.getInstance().notifyEvent(dialect);
                }
            });
        }
        else if (AIGCAction.SubmitSegments.name.equals(action)) {
            // 来自 Unit 的请求
            this.execute(new SubmitSegmentTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.Setup.name.equals(action)) {
            // 来自 Unit 的请求
            this.execute(new SetupTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.Teardown.name.equals(action)) {
            // 来自 Unit 的请求
            this.execute(new TeardownTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
    }
}
