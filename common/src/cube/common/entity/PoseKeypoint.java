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

    /**
     * 鼻子。
     */
    Nose(0),

    /**
     * 左眼。
     */
    LeftEye(1),

    /**
     * 右眼。
     */
    RightEye(2),

    /**
     * 左耳。
     */
    LeftEar(3),

    /**
     * 右耳。
     */
    RightEar(4),

    /**
     * 左肩。
     */
    LeftShoulder(5),

    /**
     * 右肩。
     */
    RightShoulder(6),

    /**
     * 左肘。
     */
    LeftElbow(7),

    /**
     * 右肘。
     */
    RightElbow(8),

    /**
     * 左腕。
     */
    LeftWrist(9),

    /**
     * 右腕。
     */
    RightWrist(10),

    /**
     * 左髋。
     */
    LeftHip(11),

    /**
     * 右髋。
     */
    RightHip(12),

    /**
     * 左膝。
     */
    LeftKnee(13),

    /**
     * 右膝。
     */
    RightKnee(14),

    /**
     * 左脚踝。
     */
    LeftAnkle(15),

    /**
     * 右脚踝。
     */
    RightAnkle(16),

    Unknown(99),

    ;

    public final int code;

    PoseKeypoint(int code) {
        this.code = code;
    }

    public static PoseKeypoint parse(int code) {
        for (PoseKeypoint pk : PoseKeypoint.values()) {
            if (pk.code == code) {
                return pk;
            }
        }
        return Unknown;
    }

    public static PoseKeypoint parse(String nameOrCode) {
        for (PoseKeypoint pk : PoseKeypoint.values()) {
            if (pk.name().equalsIgnoreCase(nameOrCode)
                    || Integer.toString(pk.code).equals(nameOrCode)) {
                return pk;
            }
        }
        return Unknown;
    }
}
