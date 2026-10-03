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
import cube.common.action.AIGCAction;
import cube.core.AbstractCellet;
import cube.core.Kernel;
import cube.service.aigc.event.EventCenter;
import cube.service.aigc.spi.ActionRunner;
import cube.service.aigc.spi.ModuleRegistry;
import cube.service.aigc.task.*;

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
     * <p>在 {@link #install()} 中创建并完成装载，早于任何一次
     * {@link #onListened} 派发，因此派发路径上不需要判空。</p>
     *
     * <p>出厂配置列出零个模块，故此处恒为「已装载但绑定表为空」，
     * 派发成本为一次 volatile 读。</p>
     */
    private ModuleRegistry moduleRegistry;

    public AIGCCellet() {
        super(AIGCService.NAME);
        this.responderList = new ConcurrentLinkedQueue<>();
    }

    @Override
    public boolean install() {
        this.service = new AIGCService(this);

        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.installModule(AIGCService.NAME, this.service);

        // 业务模块发现与动作绑定。必须在 install 阶段完成：
        // install 早于内核启动，也早于任何一次 onListened，故装载完成时路由表已权威。
        this.moduleRegistry = new ModuleRegistry(new ActionRouter());
        this.moduleRegistry.load();

        return true;
    }

    @Override
    public void uninstall() {
        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.uninstallModule(AIGCService.NAME);

        for (Responder responder : this.responderList) {
            responder.finish();
        }
        this.responderList.clear();
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
     * <p>出厂配置下列出零个模块，本方法在「是否已绑定」检查后立即返回
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

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        ActionDialect dialect = new ActionDialect(primitive);
        String action = dialect.getName();

        // 业务模块派发（双轨接入，出厂为空注册表）：
        // 未命中时立即返回 false，继续走下方的既有动作分支，行为与改造前完全一致。
        // 应答阻塞分支（Responder.NotifierKey）优先级最高，此处显式排除，确保它不会被业务模块劫持。
        if (!dialect.containsParam(Responder.NotifierKey) && this.dispatchToModule(talkContext, primitive, dialect)) {
            return;
        }

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
        else if (AIGCAction.AppQuerySchedule.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new AppQueryScheduleTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.AppUpdateSchedule.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new AppUpdateScheduleTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.AppNewSchedule.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new AppNewScheduleTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.AppDeleteSchedule.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new AppDeleteScheduleTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.AppQueryCustomer.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new AppQueryCustomerTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.AppUpdateCustomer.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new AppUpdateCustomerTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.AppNewCustomer.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new AppNewCustomerTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.AppDeleteCustomer.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new AppDeleteCustomerTask(this, talkContext, primitive,
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
        else if (AIGCAction.AnalyseVoiceStream.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new AnalyseVoiceStreamTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.QueryCounselingCaption.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new QueryCounselingCaptionTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.QueryCounselingStrategy.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new QueryCounselingStrategyTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GetVoiceStreamFile.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GetVoiceStreamTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.StopVoiceStream.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new StopVoiceStreamTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.SubmitCopilotSheet.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new SubmitCopilotSheetTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.ApplyCopilot.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new ApplyCopilotTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.DisposeCopilot.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new DisposeCopilotTask(this, talkContext, primitive,
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
        else if (AIGCAction.SpeechAnalysis.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new SpeechAnalysisTask(this, talkContext, primitive,
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
        else if (AIGCAction.PreInfer.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new PreInferTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.QueryPsychologyComprehensive.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GetPsychologyComprehensiveTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GeneratePsychologyComprehensive.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GeneratePsychologyComprehensiveTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GeneratePsychologyReport.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GeneratePsychologyReportTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GetPsychologyReport.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GetPsychologyReportTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.CheckPsychologyPainting.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new CheckPsychologyPaintingTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.ModifyReportRemark.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new ModifyReportRemarkTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.StopGeneratingPsychologyReport.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new StopGeneratingPsychologyReportTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GetPsychologyReportPart.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GetPsychologyReportPartTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GeneratePsychologyTemplateArticle.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GeneratePsychologyTemplateArticleTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GetPsychologyTemplateArticle.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GetPsychologyTemplateArticleTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.ListPsychologyScales.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new ListPsychologyScalesTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GetPsychologyScale.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GetPsychologyScaleTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GeneratePsychologyScale.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GeneratePsychologyScaleTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.SubmitPsychologyAnswerSheet.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new SubmitPsychologyAnswerSheetTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.PsychologyConversation.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new PsychologyConversationTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GetPsychologyPainting.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GetPsychologyPaintingTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.GetPaintingLabel.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GetPaintingLabelTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.SetPaintingLabel.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new SetPaintingLabelTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.SetPaintingReportState.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new SetPaintingReportStateTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.ResetReportAttention.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new ResetReportAttentionTask(this, talkContext, primitive,
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
