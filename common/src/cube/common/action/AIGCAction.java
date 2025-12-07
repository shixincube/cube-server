/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.action;

/**
 * AIGC 动作。
 */
public enum AIGCAction {

    /**
     * 服务节点加入。
     */
    Setup("setup"),

    /**
     * 服务节点离开。
     */
    Teardown("teardown"),

    /**
     * 单元向服务器提交知识分段内容。
     */
    SubmitSegments("submitSegments"),

    /**
     * 获取 AI 单元。
     */
    GetUnits("getUnits"),

    /**
     * 获取配置信息。
     */
    GetConfig("getConfig"),

    /**
     * 校验令牌。
     */
    CheckToken("checkToken"),

    /**
     * 应用层获取或创建用户。
     */
    AppGetOrCreateUser("appGetOrCreateUser"),

    /**
     * 应用层修改用户信息。
     */
    AppModifyUser("appModifyUser"),

    /**
     * 应用层校验用户。
     */
    AppCheckInUser("appCheckInUser"),

    /**
     * 应用层注销用户。
     */
    AppSignOutUser("appSignOutUser"),

    /**
     * 应用层获取用户描述数据。
     */
    AppGetUserProfile("appGetUserProfile"),

    /**
     * 应用层激活会员。
     */
    AppActivateMembership("appActivateMembership"),

    /**
     * 应用层取消会员。
     */
    AppCancelMembership("appCancelMembership"),

    /**
     * 应用层 App 版本信息。
     */
    AppVersion("appVersion"),

    /**
     * 应用层获取 ASCII Art 数据。
     */
    AppASCIIArt("appASCIIArt"),

    /**
     * 获取词云数据。
     */
    GetWordCloud("getWordCloud"),

    /**
     * 注入或者获取令牌。
     */
    AppInjectOrGetToken("injectOrGetToken"),

    /**
     * 评价回答内容的质量。
     */
    Evaluate("evaluate"),

    /**
     * 请求通道。
     */
    RequestChannel("requestChannel"),

    /**
     * 停止通道响应。
     */
    StopChannel("stopChannel"),

    /**
     * 获取通道工作信息。
     */
    GetChannelInfo("getChannelInfo"),

    /**
     * 保活通道。
     */
    KeepAliveChannel("keepAliveChannel"),

    /**
     * 添加应用事件。
     */
    AddAppEvent("addAppEvent"),

    /**
     * 查询应用事件。
     */
    QueryAppEvent("queryAppEvent"),

    /**
     * 查询用量。
     */
    QueryUsages("queryUsages"),

    /**
     * 查询互动问答历史。
     */
    QueryChatHistory("queryChatHistory"),

    /**
     * 问答互动。
     */
    Chat("chat"),

    /**
     * 会话式问答互动（异步方式）。
     */
    Conversation("conversation"),

    /**
     * 查询会话数据。
     */
    QueryConversation("queryConversation"),

    /**
     * 获取队列计数。
     */
    GetQueueCount("getQueueCount"),

    /**
     * 文本生成文本。
     */
    TextToText("textToText"),

    /**
     * 生成摘要。
     */
    Summarization("summarization"),

    /**
     * 提取关键词。
     */
    ExtractKeywords("extractKeywords"),

    /**
     * 语义搜索。
     */
    SemanticSearch("semanticSearch"),

    /**
     * 检索并重排名。
     */
    RetrieveReRank("retrieveReRank"),

    /**
     * 自动语音识别。
     */
    AutomaticSpeechRecognition("automaticSpeechRecognition"),

    /**
     * 说话人分割聚类。
     */
    SpeakerDiarization("speakerDiarization"),

    /**
     * 获取说话人分割数据清单。
     */
    ListSpeakerDiarizations("listSpeakerDiarizations"),

    /**
     * 删除说话人分析数据。
     */
    DeleteSpeakerDiarization("deleteSpeakerDiarization"),

    /**
     * 分词。
     */
    Segmentation("segmentation"),

    /**
     * 文本生成图片。
     */
    TextToImage("textToImage"),

    /**
     * 文本生成文件。
     */
    TextToFile("textToFile"),

    /**
     * 搜索命令。
     */
    SearchCommand("searchCommand"),

    /**
     * 执行知识库问答。
     */
    PerformKnowledgeQA("performKnowledgeQA"),

    /**
     * 获取知识库文档进度。
     */
    GetKnowledgeQAProgress("getKnowledgeQAProgress"),

    /**
     * 获取知识库功能的配置数据。
     */
    GetKnowledgeProfile("getKnowledgeProfile"),

    /**
     * 更新知识库功能的配置数据。
     */
    UpdateKnowledgeProfile("updateKnowledgeProfile"),

    /**
     * 获取知识框架信息。
     */
    GetKnowledgeFramework("getKnowledgeFramework"),

    /**
     * 创建知识库。
     */
    NewKnowledgeBase("newKnowledgeBase"),

    /**
     * 删除知识库。
     */
    DeleteKnowledgeBase("deleteKnowledgeBase"),

    /**
     * 更新知识库信息。
     */
    UpdateKnowledgeBase("updateKnowledgeBase"),

    /**
     * 获取知识库文档清单。
     */
    ListKnowledgeDocs("listKnowledgeDocs"),

    /**
     * 导入知识库文档的单元。
     */
    ImportKnowledgeDoc("importKnowledgeDoc"),

    /**
     * 移除导入的知识库文档。
     */
    RemoveKnowledgeDoc("removeKnowledgeDoc"),

    /**
     * 激活知识库文档。
     */
    ActivateKnowledgeDoc("activateKnowledgeDoc"),

    /**
     * 释放知识库文档。
     */
    DeactivateKnowledgeDoc("deactivateKnowledgeDoc"),

    /**
     * 获取知识分段数据。
     */
    GetKnowledgeSegments("getKnowledgeSegments"),

    /**
     * 获取知识库操作进度。
     */
    GetKnowledgeProgress("getKnowledgeProgress"),

    /**
     * 批量激活知识库文档。
     */
    BatchActivateKnowledgeDocs("batchActivateKnowledgeDocs"),

    /**
     * 批量释放知识库文档。
     */
    BatchDeactivateKnowledgeDocs("batchDeactivateKnowledgeDocs"),

    /**
     * 重置知识库数据。
     */
    ResetKnowledgeStore("resetKnowledgeStore"),

    /**
     * 删除知识库数据仓库。
     */
    DeleteKnowledgeStore("deleteKnowledgeStore"),

    /**
     * 备份知识库数据。
     */
    BackupKnowledgeStore("backupKnowledgeStore"),

    /**
     * 获取备份知识库描述。
     */
    GetBackupKnowledgeStores("getBackupKnowledgeStores"),

    /**
     * 获取重置知识库进度。
     */
    GetResetKnowledgeProgress("getResetKnowledgeProgress"),

    /**
     * 获取知识库文章清单。
     */
    ListKnowledgeArticles("listKnowledgeArticles"),

    /**
     * 更新知识库文章数据。
     */
    UpdateKnowledgeArticle("updateKnowledgeArticle"),

    /**
     * 新增知识库文章。
     */
    AppendKnowledgeArticle("appendKnowledgeArticle"),

    /**
     * 删除知识库文章。
     */
    RemoveKnowledgeArticle("removeKnowledgeArticle"),

    /**
     * 对指定联系人激活知识库文章。
     */
    ActivateKnowledgeArticle("activateKnowledgeArticle"),

    /**
     * 对指定联系人释放知识库文章。
     */
    DeactivateKnowledgeArticle("deactivateKnowledgeArticle"),

    /**
     * 查询所有文章的分类。
     */
    QueryAllArticleCategories("queryAllArticleCategories"),

    /**
     * 生成知识。
     */
    GenerateKnowledge("generateKnowledge"),

    /**
     * 提取 URL 内容。
     */
    ExtractURLContent("extractURLContent"),

    /**
     * 获取搜索结果。
     */
    GetSearchResults("getSearchResults"),

    /**
     * 获取上下文的推理内容。
     */
    GetContextInference("getContextInference"),

    /**
     * 操作图表数据。
     */
    ChartData("chartData"),

    /**
     * 获取提示词。
     */
    GetPrompts("getPrompts"),

    /**
     * 设置提示词。
     */
    SetPrompts("setPrompts"),

    /**
     * 提交附件事件。
     */
    SubmitEvent("submitEvent"),

    /**
     * 预推理。
     */
    PreInfer("preInfer"),

    /**
     * 生成心理学报告。
     */
    GeneratePsychologyReport("generatePsychologyReport"),

    /**
     * 停止心理学报告生成。
     */
    StopGeneratingPsychologyReport("stopGeneratingPsychologyReport"),

    /**
     * 获取心理学报告。
     */
    GetPsychologyReport("getPsychologyReport"),

    /**
     * 获取心理学报告指定部分的内容。
     */
    GetPsychologyReportPart("getPsychologyReportPart"),

    /**
     * 心理学绘画预测。
     */
    PredictPsychologyPainting("predictPsychologyPainting"),

    /**
     * 心理学指标因子预测。
     */
    PredictPsychologyFactors("predictPsychologyFactors"),

    /**
     * 检查绘画。
     */
    CheckPsychologyPainting("checkPsychologyPainting"),

    /**
     * 重置报告关注度。
     */
    ResetReportAttention("resetReportAttention"),

    /**
     * 修改报告备注。
     */
    ModifyReportRemark("modifyReportRemark"),

    /**
     * 获取心理学量表列表。
     */
    ListPsychologyScales("listPsychologyScales"),

    /**
     * 获取心理学量表。
     */
    GetPsychologyScale("getPsychologyScale"),

    /**
     * 生成心理学量表。
     */
    GeneratePsychologyScale("generatePsychologyScale"),

    /**
     * 提交量表答题卡。
     */
    SubmitPsychologyAnswerSheet("submitPsychologyAnswerSheet"),

    /**
     * 心理学对话。
     */
    PsychologyConversation("psychologyConversation"),

    /**
     * 获取心理学绘画数据。
     */
    GetPsychologyPainting("getPsychologyPainting"),

    /**
     * 获取绘画标注。
     */
    GetPaintingLabel("getPaintingLabel"),

    /**
     * 设置绘画标注。
     */
    SetPaintingLabel("setPaintingLabel"),

    /**
     * 设置绘画报告状态。
     */
    SetPaintingReportState("setPaintingReportState"),

    /**
     * 语音情绪识别。
     */
    SpeechEmotionRecognition("speechEmotionRecognition"),

    /**
     * 面部表情识别。
     */
    FacialExpressionRecognition("facialExpressionRecognition"),

    /**
     * 获取情绪记录。
     */
    GetEmotionRecords("getEmotionRecords"),

    /**
     * 音频流分析。
     */
    AnalyseAudioStream("analyseAudioStream"),

    ;

    public final String name;

    AIGCAction(String name) {
        this.name = name;
    }
}
