/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

/**
 * 姿态关键点。
 */
public enum PoseKeypoint {

    Nose(0),

    LeftEye(1),

    right_eye(2),

    left_ear(3),

    right_ear(4),

    left_shoulder(5),

    right_shoulder(6),

    left_elbow(7),

    right_elbow(8),

    left_wrist(9),

    right_wrist(10),

    left_hip(11),

    right_hip(12),

    left_knee(13),

    right_knee(14),

    left_ankle(15),

    right_ankle(16),

    ;

    public final int code;

    PoseKeypoint(int code) {
        this.code = code;
    }
}
