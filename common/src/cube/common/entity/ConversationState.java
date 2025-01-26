/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

/**
 * 会话的状态。
 */
public enum ConversationState {

    /**
     * 正常状态。
     */
    Normal(1),

    /**
     * 重要的或置顶的状态。
     */
    Important(2),

    /**
     * 已删除状态。
     */
    Deleted(3),

    /**
     * 已销毁状态。
     */
    Destroyed(4);

    public final int code;

    ConversationState(int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return Integer.toString(this.code);
    }

    public static ConversationState parse(int code) {
        for (ConversationState state : ConversationState.values()) {
            if (state.code == code) {
                return state;
            }
        }

        return Deleted;
    }
}
