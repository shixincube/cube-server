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
     * 注入或者获取令牌。
     */
    InjectOrGetToken("injectOrGetToken"),

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
     * 问答互动。
     */
    Chat("chat"),

    /**
     * 增强型会话。
     */
    Conversation("conversation"),

    /**
     * 情感分析。
     */
    Sentiment("sentiment"),

    /**
     * 生成摘要。
     */
    Summarization("summarization"),

    /**
     * 提取关键词。
     */
    ExtractKeywords("extractKeywords"),

    /**
     * 自然语言通用任务。
     */
    NaturalLanguageTask("naturalLanguageTask"),

    /**
     * 自动语音识别。
     */
    AutomaticSpeechRecognition("automaticSpeechRecognition"),

    /**
     * 分词。
     */
    Segmentation("segmentation"),

    /**
     * 文本生成图片。
     */
    TextToImage("textToImage"),

    /**
     * 搜索命令。
     */
    SearchCommand("searchCommand"),

    /**
     * 获取知识库功能的概述数据。
     */
    GetKnowledgeProfile("getKnowledgeProfile"),

    /**
     *
     */
    UpdateKnowledgeProfile("updateKnowledgeProfile"),

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
     * 获取知识库文章清单。
     */
    ListKnowledgeArticles("listKnowledgeArticles"),

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
     * 生成提示词。
     */
    GeneratePrompt("generatePrompt"),

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
     * 操作舆情数据。
     */
    PublicOpinionData("publicOpinionData"),

    /**
     * 通过功能模块进行推理。
     */
    InferByModule("inferByModule"),

    /**
     * 预推理。
     */
    PreInfer("preInfer"),

    /**
     * 预测图片元素。
     */
    PredictImage("predictImage"),

    /**
     * 生成心理学报告。
     */
    GeneratePsychologyReport("generatePsychologyReport"),

    ;

    public final String name;

    AIGCAction(String name) {
        this.name = name;
    }
}
