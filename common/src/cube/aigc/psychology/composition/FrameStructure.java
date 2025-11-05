/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

/**
 * 框架结构。
 */
public enum FrameStructure {

    /**
     * 整体上方空间。
     */
    WholeTopSpace("WholeTopSpace"),

    /**
     * 整体下方空间。
     */
    WholeBottomSpace("WholeBottomSpace"),

    /**
     * 整体左边空间。
     */
    WholeLeftSpace("WholeLeftSpace"),

    /**
     * 整体右边空间。
     */
    WholeRightSpace("WholeRightSpace"),

    /**
     * 中间左上部空间。
     */
    CenterTopLeftSpace("CenterTopLeftSpace"),

    /**
     * 中间右上部空间。
     */
    CenterTopRightSpace("CenterTopRightSpace"),

    /**
     * 中间左下部空间。
     */
    CenterBottomLeftSpace("CenterBottomLeftSpace"),

    /**
     * 中间右下部空间。
     */
    CenterBottomRightSpace("CenterBottomRightSpace"),

    /**
     * 左上角。
     */
    TopLeftCorner("TopLeftCorner"),

    /**
     * 右上角。
     */
    TopRightCorner("TopRightCorner"),

    /**
     * 左下角。
     */
    BottomLeftCorner("BottomLeftCorner"),

    /**
     * 右下角。
     */
    BottomRightCorner("BottomRightCorner"),

    Normal("Normal");

    public final String name;

    FrameStructure(String name) {
        this.name = name;
    }
}
