/*
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

package cube.service.riskmgmt;

/**
 * 敏感词。
 */
public class SensitiveWord {

    public enum SensitiveWordType {
        /**
         * 基于产品规则不被允许。
         */
        Unallowed(1),

        /**
         * 低俗的。
         */
        Degraded(2),

        /**
         * 违法的。
         */
        Illegal(3),

        /**
         * 其他因素。
         */
        Other(9);

        public final int code;

        SensitiveWordType(int code) {
            this.code = code;
        }

        public static SensitiveWordType parse(int code) {
            if (code == Unallowed.code) return Unallowed;
            else if (code == Degraded.code) return Degraded;
            else if (code == Illegal.code) return Illegal;
            else return Other;
        }
    };

    public final String word;

    public final SensitiveWordType type;

    public SensitiveWord(String word, SensitiveWordType type) {
        this.word = word.toUpperCase();
        this.type = type;
    }

    public SensitiveWord(String word, int code) {
        this.word = word.toUpperCase();
        this.type = SensitiveWordType.parse(code);
    }

    @Override
    public boolean equals(Object object) {
        if (null != object && object instanceof SensitiveWord) {
            SensitiveWord other = (SensitiveWord) object;
            if (other.word.equalsIgnoreCase(this.word)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.word.hashCode();
    }
}
