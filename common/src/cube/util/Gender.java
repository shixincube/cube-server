/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.util;

/**
 * 性别。
 */
public enum Gender {

    /**
     * 男性。
     */
    Male("male"),

    /**
     * 女性。
     */
    Female("female"),

    /**
     * 未知。
     */
    Unknown("unknown")

    ;

    public final String name;

    Gender(String name) {
        this.name = name;
    }

    public static Gender parse(String name) {
        for (Gender gender : Gender.values()) {
            if (gender.name.equalsIgnoreCase(name)) {
                return gender;
            }
        }
        return Gender.Unknown;
    }
}
