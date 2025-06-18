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
import cube.common.action.AIGCAction;
import cube.core.AbstractCellet;
import cube.core.Kernel;
import cube.service.aigc.task.*;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * AIGC 服务单元。
 */
public class AIGCCellet extends AbstractCellet {

    private AIGCService service;

    private ConcurrentLinkedQueue<Responder> responderList;

    public AIGCCellet() {
        super(AIGCService.NAME);
        this.responderList = new ConcurrentLinkedQueue<>();
    }

    @Override
    public boolean install() {
        this.service = new AIGCService(this);

        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.installModule(AIGCService.NAME, this.service);

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

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        ActionDialect dialect = new ActionDialect(primitive);
        String action = dialect.getName();

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
//        else if (AIGCAction.Sentiment.name.equals(action)) {
//            // 来自 Dispatcher 的请求
//            this.execute(new SentimentTask(this, talkContext, primitive,
//                    this.markResponseTime(action)));
//        }
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
        else if (AIGCAction.Conversation.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new ConversationTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (AIGCAction.QueryConversation.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new QueryConversationTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
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
        else if (AIGCAction.SearchCommand.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new SearchCommandTask(this, talkContext, primitive,
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
        else if (AIGCAction.GetPsychologyScoreBenchmark.name.equals(action)) {
            // 来自 Dispatcher 的请求
            this.execute(new GetPsychologyScoreBenchmarkTask(this, talkContext, primitive,
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
