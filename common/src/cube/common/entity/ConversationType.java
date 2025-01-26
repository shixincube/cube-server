/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

/**
 * 会话类型。
 */
public enum ConversationType {

    /**
     * 与联系人的会话。
     */
    Contact(1),

    /**
     * 与群组的会话。
     */
    Group(2),

    /**
     * 与组织的会话。
     */
    Organization(3),

    /**
     * 系统类型会话。
     */
    System(4),

    /**
     * 通知类型会话。
     */
    Notifier(5),

    /**
     * 助手类型会话。
     */
    Assistant(6),

    /**
     * 其他会话类型。
     */
    Other(9);

    public final int code;

    ConversationType(int code) {
        this.code = code;
    }

    public static ConversationType parse(int code) {
        for (ConversationType type : ConversationType.values()) {
            if (type.code == code) {
                return type;
            }
        }

        return Other;
    }
}
