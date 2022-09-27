/*
 * This source file is part of Cube.
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

import cube.common.entity.TextConstraint;
import cube.vision.Size;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文本实用函数库。
 */
public final class TextUtils {

    // Pattern.compile("^-?[0-9]+");
    private static final Pattern sPatternNumeric = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    private TextUtils() {
    }

    /**
     * 判断字符串是否全是数字。
     * @param str
     * @return
     */
    public static boolean isNumeric(String str) {
        Matcher isNum = sPatternNumeric.matcher(str);
        if (!isNum.matches()) {
            return false;
        }
        return true;
    }

    public static JSONObject parseUserAgent(String text) {
        JSONObject json = new JSONObject();
        return json;
    }

    /**
     * 度量文本区域面积。
     * @param text
     * @param constraint
     * @param wordSpacing
     * @param lineSpacing
     * @param maxWidth
     * @param maxHeight
     * @return
     */
    public static Size measureTextAreas(String text, TextConstraint constraint,
                                         int wordSpacing, int lineSpacing,
                                         int maxWidth, int maxHeight) {
        Size areas = new Size(maxWidth, maxHeight);

        int row = 1;
        int col = 0;
        // 解析文本行列数量
        int numChar = 0;
        for (int i = 0, len = text.length(); i < len; ++i) {
            char c = text.charAt(i);
            if (c == '\n') {
                ++row;
                if (numChar > col) {
                    col = numChar;
                }
                numChar = 0;
            }
            else {
                ++numChar;
            }
        }
        if (col == 0 || col < numChar) {
            col = numChar;
        }

        int ps = constraint.pointSize;

        // 字符总宽度 + 总字符间距
        areas.width = ps * col + wordSpacing * (col - 1);

        // 字符总高度 + 总行间距
        areas.height = ps * row + lineSpacing * (row - 1);

        // 上下和左右都各留一个字符的 Padding
        areas.width += ps + ps;
        areas.height += ps + ps;

        if (areas.width > maxWidth) {
            areas.width = maxWidth;
        }

        if (areas.height > maxHeight) {
            areas.height = maxHeight;
        }

        return areas;
    }
}
