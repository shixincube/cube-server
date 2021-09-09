/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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
 * 幸运数字。
 */
public class LuckyNumbers {

    // TODO 连号模板
    private final static String[] sTemplates = new String[] {
            "AABBCCDD",
            "AAABBBCC",
            "AABBBCCC",
            "AAABBCCC",
            "ABABABAB",
            "ABCDABCD",
            "AAAABBCC",
            "AAAABBBC",
            "ABBBCCCC",
            "AABBBBCC",
            "AABBCCCC",
            "ABBBABBB",
            "AAABAAAB",
            "AAAAXXXX",
            "ABABXXXX",
            "XXXXAAAA",
            "XXXXABAB",
            "XXAAAAXX",
            "XXABABXX",
            "AXAXAXAX" };

    private LuckyNumbers() {
    }

    /**
     * 生成随机幸运数字。
     *
     * @return
     */
    public static long make() {
        int A = LuckyNumbers.rand();
        int B = LuckyNumbers.rand();
        int C = LuckyNumbers.rand();
        int D = LuckyNumbers.rand();

        // 随机模板
        int mod = (int) (Math.round(Math.random() * 1000) % LuckyNumbers.sTemplates.length);
        String template = LuckyNumbers.sTemplates[mod];

        int[] result = new int[template.length()];
        for (int i = 0; i < result.length; ++i) {
            if (template.charAt(i) == 'A') result[i] = A;
            else if (template.charAt(i) == 'B') result[i] = B;
            else if (template.charAt(i) == 'C') result[i] = C;
            else if (template.charAt(i) == 'D') result[i] = D;
            else result[i] = LuckyNumbers.rand();
        }

        // 使用 while 循环防止随机数再次生成 0
        while (0 == result[0]) {
            result[0] = LuckyNumbers.rand();
        }

        StringBuilder buf = new StringBuilder();
        for (int n : result) {
            buf.append(Integer.toString(n));
        }

        return Long.parseLong(buf.toString());
    }

    private static int rand() {
        return (int) (Math.round(Math.random() * 1000) % 10);
    }
}
