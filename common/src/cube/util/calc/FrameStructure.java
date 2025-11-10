/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util.calc;

/**
 * 框架结构。
 */
public enum FrameStructure {

    /**
     * 整体上方空间。
     */
    TopSpace("TopSpace"),

    /**
     * 整体下方空间。
     */
    BottomSpace("BottomSpace"),

    /**
     * 整体左边空间。
     */
    LeftSpace("LeftSpace"),

    /**
     * 整体右边空间。
     */
    RightSpace("RightSpace"),

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

    /**
     * 不在角落。
     */
    NotInCorner("NotInCorner"),

    Normal("Normal");

    public final String name;

    FrameStructure(String name) {
        this.name = name;
    }
}
