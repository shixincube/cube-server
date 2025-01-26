/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.action;

/**
 * 文件处理动作。
 */
public enum FileProcessorAction {

    /**
     * 获取媒体源地址。
     */
    GetMediaSource("getMediaSource"),

    /**
     * 提交工作流。
     */
    SubmitWorkflow("submitWorkflow"),

    /**
     * 取消工作流。
     */
    CancelWorkflow("cancelWorkflow"),

    /**
     * 工作流操作中。
     */
    WorkflowOperating("workflowOperating"),

    /**
     * 图像文件操作。
     */
    Image("image"),

    /**
     * 视频文件操作。
     */
    Video("video"),

    /**
     * 音频文件操作。
     */
    Audio("audio"),

    /**
     * 字符识别。
     */
    OCR("ocr"),

    /**
     * 生成缩略图。
     */
    Thumb("thumb"),

    /**
     * 办公文档转换。
     */
    OfficeConvertTo("officeConvertTo"),

    /**
     * 对象检测。
     */
    DetectObject("detectObject"),

    /**
     * 对象检测应答。
     */
    DetectObjectAck("detectObjectAck"),

    /**
     * 隐写。
     */
    Steganographic("steganographic"),

    /**
     * 读取 Excel 文件。
     */
    ReadExcel("readExcel"),

    /**
     * 未知动作。
     */
    Unknown("")

    ;

    public final String name;

    FileProcessorAction(String name) {
        this.name = name;
    }
}
