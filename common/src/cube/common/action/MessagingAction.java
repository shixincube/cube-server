/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
