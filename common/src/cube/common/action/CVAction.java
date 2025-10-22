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
     * 合并条形码。
     */
    CombineBarcodes("combineBarcodes"),

    /**
     * 检测条形码。
     */
    DetectBarCode("detectBarCode"),

    /**
     * 图像物体检测。
     */
    ObjectDetection("objectDetection"),

    /**
     * 姿态估算。
     */
    PoseEstimation("poseEstimation"),

    /**
     * 剪裁照片中的纸张。
     */
    ClipPaper("clipPaper"),

    ;

    public final String name;

    CVAction(String name) {
        this.name = name;
    }
}
