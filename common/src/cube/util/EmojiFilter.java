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

package cube.util;

import org.json.JSONObject;

public class EmojiFilter {

    private static boolean isEmojiCharacter(char codePoint) {
        return (codePoint == 0x0) || (codePoint == 0x9) || (codePoint == 0xA)
                || (codePoint == 0xD)
                || ((codePoint >= 0x20) && (codePoint <= 0xD7FF))
                || ((codePoint >= 0xE000) && (codePoint <= 0xFFFD))
                || ((codePoint >= 0x10000) && (codePoint <= 0x10FFFF));
    }

    /**
     * 过滤 emoji 或者其他非文字类型的字符
     * @param source
     * @return
     */
    public static String filterEmoji(String source) {
        if (TextUtils.isBlank(source)) {
            return source;
        }

        StringBuilder buf = null;
        int len = source.length();
        for (int i = 0; i < len; i++) {
            char codePoint = source.charAt(i);
            if (isEmojiCharacter(codePoint)) {
                if (buf == null) {
                    buf = new StringBuilder(source.length());
                }
                buf.append(codePoint);
            }
        }

        if (buf == null) {
            return source;
        }
        else {
            if (buf.length() == len) {
                buf = null;
                return source;
            } else {
                return buf.toString();
            }
        }
    }

    public static void main(String[] args) {
        String str1 = "你好👋！很高兴见到你，欢迎问我任何问题。";
        String str2 = "我没有表情符号，No EMO";

        String r1 = EmojiFilter.filterEmoji(str1);
        System.out.println(r1);

        String r2 = EmojiFilter.filterEmoji(str2);
        System.out.println(r2);

        System.out.println("--------------------------------------------------------------------");

        String strJson = "{\"unit\":\"Chat\",\"code\":0,\"answer\":\"你好👋！很高兴见到你，欢迎问我任何问题。\",\"query\":\"你好\",\"context\":{\"inferable\":false,\"domain\":\"\",\"resources\":[],\"networkingInferEnd\":false,\"networking\":false,\"id\":2747657682,\"type\":\"simplex\",\"inferring\":false,\"searchable\":true,\"timestamp\":1707097484378},\"sn\":457725144,\"timestamp\":1707097485503}";
        JSONObject json = new JSONObject(strJson);
        System.out.println(json.toString(4));
        System.out.println("--------------------------------------------------------------------");
        String rStrJson = EmojiFilter.filterEmoji(strJson);
        JSONObject rJson = new JSONObject(rStrJson);
        System.out.println(rJson.toString(4));
    }
}