/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

/**
 * 消息状态。
 */
public enum MessageState {

    /**
     * 消息处理失败。
     */
    Fault(1),

    /**
     * 未发送状态。
     */
    Unsent(5),

    /**
     * 正在发送状态。
     */
    Sending(9),

    /**
     * 已发送状态。
     */
    Sent(10),

    /**
     * 已被阅读状态。
     */
    Read(20),

    /**
     * 被拒绝阅读状态。
     */
    Forbidden(22),

    /**
     * 已撤回。
     */
    Retracted(30),

    /**
     * 已删除。
     */
    Deleted(40),

    /**
     * 被阻止发送。
     */
    SendBlocked(51),

    /**
     * 被阻止接收。
     */
    ReceiveBlocked(52),

    /**
     * 被系统阻断发送。
     */
    SystemBlocked(53),

    /**
     * 未知状态。
     */
    Unknown(0);

    public final int code;

    MessageState(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

    @Override
    public String toString() {
        return Integer.toString(this.code);
    }

    public static MessageState parse(int code) {
        for (MessageState state : MessageState.values()) {
            if (state.code == code) {
                return state;
            }
        }

        return MessageState.Unknown;
    }
}
