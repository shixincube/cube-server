/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology;

public enum Role {

    /**
     * 客户/来访者。
     */
    Customer("customer"),

    /**
     * 咨询师。
     */
    Counselor("counselor"),

    /**
     * 其他。
     */
    Other("other"),

    ;

    public final String label;

    Role(String label) {
        this.label = label;
    }

    public static Role parse(String label) {
        for (Role role : Role.values()) {
            if (role.label.equalsIgnoreCase(label)) {
                return role;
            }
        }
        return Other;
    }
}
