/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.riskmgmt.util;

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
