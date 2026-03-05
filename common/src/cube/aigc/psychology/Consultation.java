/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology;

/**
 * 咨询形式。
 */
public enum Consultation {

    /**
     * 面对面。
     */
    FaceToFace(0),

    /**
     * 线上咨询。
     */
    Online(1),

    ;

    public final int code;

    Consultation(int code) {
        this.code = code;
    }

    public static Consultation parse(int code) {
        for (Consultation consultation : Consultation.values()) {
            if (consultation.code == code) {
                return consultation;
            }
        }
        return Consultation.FaceToFace;
    }
}
