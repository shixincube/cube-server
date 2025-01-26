/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.util;

/**
 * 电话号码辅助函数。
 */
public class PhoneNumbers {

    private PhoneNumbers() {
    }

    public static String desensitize(String phoneNumber) {
        if (phoneNumber.length() == 11) {
            return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(7);
        }
        else if (phoneNumber.length() <= 4) {
            return "";
        }
        else {
            StringBuilder buf = new StringBuilder(phoneNumber.substring(0, 2));
            for (int i = 0, len = phoneNumber.length() - 4; i < len; ++i) {
                buf.append("*");
            }
            buf.append(phoneNumber.substring(phoneNumber.length() - 2));
            return buf.toString();
        }
    }
}
