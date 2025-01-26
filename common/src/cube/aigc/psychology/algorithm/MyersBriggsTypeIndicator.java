/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.algorithm;

public enum MyersBriggsTypeIndicator {

    /**
     * 内倾。
     */
    Introversion("内倾", "I"),

    /**
     * 外倾。
     */
    Extraversion("外倾", "E"),

    /**
     * 感觉。
     */
    Sensing("感觉", "S"),

    /**
     * 直觉。
     */
    Intuition("直觉", "N"),

    /**
     * 情感。
     */
    Feeling("情感", "F"),

    /**
     * 思考。
     */
    Thinking("思考", "T"),

    /**
     * 判断。
     */
    Judging("判断", "J"),

    /**
     * 知觉。
     */
    Perceiving("知觉", "P")

    ;

    public final String displayName;

    public final String code;

    MyersBriggsTypeIndicator(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }

    public static MyersBriggsTypeIndicator parse(String nameOrCode) {
        for (MyersBriggsTypeIndicator indicator : MyersBriggsTypeIndicator.values()) {
            if (indicator.displayName.equals(nameOrCode) || indicator.code.equalsIgnoreCase(nameOrCode)) {
                return indicator;
            }
        }

        return null;
    }
}
