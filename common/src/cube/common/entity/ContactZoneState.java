/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

/**
 * 联系人分区状态。
 */
public enum ContactZoneState {

    /**
     * 正常状态。
     */
    Normal(0),

    /**
     * 已移除。
     */
    Deleted(1);

    public final int code;

    ContactZoneState(int code) {
        this.code = code;
    }

    public static ContactZoneState parse(int code) {
        for (ContactZoneState state : ContactZoneState.values()) {
            if (state.code == code) {
                return state;
            }
        }

        return Deleted;
    }
}
