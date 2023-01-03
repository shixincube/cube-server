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
 * 消息模块动作定义。
 */
public enum MessagingAction {

    /**
     * 将消息推送给指定目标。
     */
    Push("push"),

    /**
     * 从自己的消息队列里获取消息。
     */
    Pull("pull"),

    /**
     * 通知接收方有消息送达。
     */
    Notify("notify"),

    /**
     * 转发消息。
     */
    Forward("forward"),

    /**
     * 撤回消息。
     */
    Retract("retract"),

    /**
     * 双向撤回消息。
     */
    RetractBoth("retractBoth"),

    /**
     * 删除消息。
     */
    Delete("delete"),

    /**
     * 标记已读。
     */
    Read("read"),

    /**
     * 焚毁消息内容。
     */
    Burn("burn"),

    /**
     * 查询消息状态。
     */
    QueryState("queryState"),

    /**
     * 获取会话列表。
     */
    GetConversations("getConversations"),

    /**
     * 更新会话数据。
     */
    UpdateConversation("updateConversation"),

    /**
     * 清空所有数据。
     */
    Cleanup("cleanup"),

    /**
     * 未知动作。
     */
    Unknown("")

    ;

    public final String name;

    MessagingAction(String name) {
        this.name = name;
    }
}
