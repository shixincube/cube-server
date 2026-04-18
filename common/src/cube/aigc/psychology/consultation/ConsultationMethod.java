/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology.consultation;

/**
 * 咨询形式。
 */
public enum ConsultationMethod {

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

    ConsultationMethod(int code) {
        this.code = code;
    }

    public static ConsultationMethod parse(int code) {
        for (ConsultationMethod consultation : ConsultationMethod.values()) {
            if (consultation.code == code) {
                return consultation;
            }
        }
        return ConsultationMethod.FaceToFace;
    }
}
