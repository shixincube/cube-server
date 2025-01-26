/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

/**
 * 联系人分区成员状态。
 */
public enum ContactZoneParticipantState {

    /**
     * 正常状态。
     */
    Normal(0),

    /**
     * 待处理状态。
     */
    Pending(1),

    /**
     * 已知待处理状态。
     */
    KnownPending(2),

    /**
     * 拒绝。
     */
    Reject(3);

    public final int code;

    ContactZoneParticipantState(int code) {
        this.code = code;
    }

    public static ContactZoneParticipantState parse(int code) {
        for (ContactZoneParticipantState state : ContactZoneParticipantState.values()) {
            if (state.code == code) {
                return state;
            }
        }
        return ContactZoneParticipantState.Normal;
    }
}
