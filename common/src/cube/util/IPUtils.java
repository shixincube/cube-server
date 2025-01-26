/*
 * This file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IP 辅助函数库。
 */
public final class IPUtils {

    private IPUtils() {
    }

    /**
     * 判断是否是合规的 IPv4 格式字符串。
     *
     * @param address
     * @return
     */
    public static boolean isIPv4(String address) {
        if (address.length() < 7 || address.length() > 15) {
            return false;
        }

        String reg = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
        Pattern pat = Pattern.compile(reg);
        Matcher mat = pat.matcher(address);
        boolean result = mat.find();
        if (result) {
            String ips[] = address.split("\\.");
            if (ips.length == 4) {
                try {
                    for (String ip : ips) {
                        int nIP = Integer.parseInt(ip);
                        if (nIP < 0 || nIP > 255) {
                            return false;
                        }
                    }
                } catch (Exception e) {
                    return false;
                }

                // 是合规的 IPv4 格式
                return true;
            }
            else {
                return false;
            }
        }
        else {
            return false;
        }
    }
}
