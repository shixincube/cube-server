/*
 * This file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
