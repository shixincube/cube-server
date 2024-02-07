/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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
