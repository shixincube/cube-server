/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.contact;

/**
 * 联系人标记。
 */
public enum ContactMask {

    Deprecated("Deprecated"),

    SignOut("SignOut")

    ;

    public final String mask;

    ContactMask(String mask) {
        this.mask = mask;
    }

    public static ContactMask parse(String name) {
        for (ContactMask mask : ContactMask.values()) {
            if (mask.mask.equalsIgnoreCase(name)) {
                return mask;
            }
        }
        return null;
    }
}
