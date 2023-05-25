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
     * 评价回答内容的质量。
     */
    Evaluate("evaluate"),

    /**
     * 请求通道。
     */
    RequestChannel("requestChannel"),

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
     * 自然语言通用任务。
     */
    NaturalLanguageTask("naturalLanguageTask"),

    /**
     * 自动语音识别。
     */
    AutomaticSpeechRecognition("automaticSpeechRecognition"),

    /**
     * 搜索命令。
     */
    SearchCommand("searchCommand"),

    /**
     * 文档问答。
     */
    DocQA("docQA")

    ;

    public final String name;

    AIGCAction(String name) {
        this.name = name;
    }
}
