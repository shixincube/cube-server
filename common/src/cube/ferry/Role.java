/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.ferry;

/**
 * 成员角色。
 */
public enum Role {

    /**
     * 管理员。
     */
    Administrator(1),

    /**
     * 普通成员。
     */
    Member(9),

    /**
     * 未知角色。
     */
    Unknown(0)

    ;

    public final int code;

    Role(int code) {
        this.code = code;
    }

    public static Role parse(int code) {
        for (Role role : Role.values()) {
            if (role.code == code) {
                return role;
            }
        }

        return Unknown;
    }
}
