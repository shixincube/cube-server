/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common;

/**
 * 语言。
 */
public enum Language {

    /**
     * 中文。
     */
    Chinese("cn"),

    /**
     * 英文。
     */
    English("en"),

    /**
     * 其他。
     */
    Other(""),

    ;

    public final String simplified;

    Language(String simplified) {
        this.simplified = simplified;
    }

    public static Language parse(String name) {
        for (Language language : Language.values()) {
            if (language.simplified.equalsIgnoreCase(name) || language.name().equalsIgnoreCase(name)) {
                return language;
            }
        }
        return Other;
    }
}
