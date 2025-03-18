/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.action;

/**
 * CV 动作。
 */
public enum CVAction {

    Setup("setup"),

    Teardown("teardown"),

    /**
     * 生成条形码。
     */
    MakeBarCode("makeBarCode"),

    /**
     * 检测条形码。
     */
    DetectBarCode("detectBarCode"),

    /**
     * 图像物体检测。
     */
    ObjectDetection("objectDetection"),

    FindContours("findContours"),

    ;

    public final String name;

    CVAction(String name) {
        this.name = name;
    }
}
