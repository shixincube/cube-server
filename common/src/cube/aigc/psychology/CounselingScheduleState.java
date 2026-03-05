/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology;

/**
 * 咨询日程状态。
 */
public enum CounselingScheduleState {

    /**
     * 已预约。
     */
    Reserved(1),

    /**
     * 已取消。
     */
    Canceled(2),

    /**
     * 已过期。
     */
    Expired(3),

    /**
     * 已完成。
     */
    Completed(4),

    /**
     * 已删除。
     */
    Deleted(9),

    ;

    public final int code;

    CounselingScheduleState(int code) {
        this.code = code;
    }

    public static CounselingScheduleState parse(int code) {
        for (CounselingScheduleState state : CounselingScheduleState.values()) {
            if (state.code == code) {
                return state;
            }
        }
        return CounselingScheduleState.Expired;
    }
}
