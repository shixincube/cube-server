/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

/**
 * 群状态。
 */
public enum GroupState {

    /**
     * 正常状态。
     */
    Normal(0),

    /**
     * 解散状态。
     */
    Dismissed(1),

    /**
     * 禁用状态。
     */
    Forbidden(2),

    /**
     * 高风险状态。
     */
    HighRisk(3),

    /**
     * 失效状态。
     */
    Disabled(9),

    /**
     * 未知状态。
     */
    Unknown(-1);

    public final int code;

    GroupState(int code) {
        this.code = code;
    }

    public static GroupState parse(int code) {
        switch (code) {
            case 0:
                return Normal;
            case 1:
                return Dismissed;
            case 2:
                return Forbidden;
            case 3:
                return HighRisk;
            case 9:
                return Disabled;
            default:
                return Unknown;
        }
    }
}
