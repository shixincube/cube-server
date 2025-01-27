/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.ferry;

/**
 * 加入方式。
 */
public enum JoinWay {

    /**
     * 使用二维码。
     */
    QRCode(1),

    /**
     * 使用邀请码。
     */
    InvitationCode(2),

    /**
     * 使用邀请二维码。
     */
    InvitationQRCode(3),

    /**
     * 未知。
     */
    Unknown(0)

    ;

    public final int code;

    JoinWay(int code) {
        this.code = code;
    }

    public static JoinWay parse(int code) {
        for (JoinWay way : JoinWay.values()) {
            if (way.code == code) {
                return way;
            }
        }

        return Unknown;
    }
}
