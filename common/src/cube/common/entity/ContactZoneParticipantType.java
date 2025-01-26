/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

/**
 * 参与人类型。
 */
public enum ContactZoneParticipantType {

    /**
     * 联系人。
     */
    Contact(1),

    /**
     * 群组。
     */
    Group(2),

    /**
     * 组织。
     */
    Organization(3),

    /**
     * 系统。
     */
    System(4),

    /**
     * 会议。
     */
    Conference(5),

    /**
     * 其他会话类型。
     */
    Other(9);


    public final int code;

    ContactZoneParticipantType(int code) {
        this.code = code;
    }

    public static ContactZoneParticipantType parse(int code) {
        for (ContactZoneParticipantType type : ContactZoneParticipantType.values()) {
            if (type.code == code) {
                return type;
            }
        }

        return Other;
    }
}
