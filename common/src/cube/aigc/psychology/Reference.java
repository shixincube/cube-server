/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology;

/**
 * 关注参考。
 */
public enum Reference {

    /**
     * 常态。
     */
    Normal("Normal"),

    /**
     * 非常态。
     */
    Abnormal("Abnormal");


    public final String name;

    Reference(String name) {
        this.name = name;
    }

    public final static Reference parse(String name) {
        if (Normal.name.equalsIgnoreCase(name)) {
            return Normal;
        }
        else {
            return Abnormal;
        }
    }
}
