/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
