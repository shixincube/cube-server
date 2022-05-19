/*
 * This source file is part of Cube.
 * <p>
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2020-2022 Cube Team.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.util;

import java.nio.charset.StandardCharsets;

/**
 * 编码串辅助函数库。
 */
public class CodeUtils {

    private CodeUtils() {
    }

    public static String extractProtocol(String codeString) {
        byte[] bytes = codeString.getBytes(StandardCharsets.UTF_8);
        byte[] buf = new byte[bytes.length];
        int length = 0;
        for (int i = 0; i < bytes.length; ++i) {
            if (bytes[i] == ':') {
                break;
            }

            buf[i] = bytes[i];
            ++length;
        }
        return new String(buf, 0, length, StandardCharsets.UTF_8);
    }

    public static String[] extractResourceSegments(String codeString) {
        int index = codeString.indexOf("//");
        if (index < 0) {
            return null;
        }

        String string = codeString.substring(index + 2);
        return string.split("\\.");
    }

    public static void main(String[] args) {
        String string = "cube://domain.demo-ferryhouse-cube";
        String protocol = CodeUtils.extractProtocol(string);
        System.out.println("Protocol: " + protocol);

        String[] segments = CodeUtils.extractResourceSegments(string);
        System.out.println(segments[0]);
        System.out.println(segments[1]);
    }
}
