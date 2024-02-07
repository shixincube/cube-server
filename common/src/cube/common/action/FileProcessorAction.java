/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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
     * 未知动作。
     */
    Unknown("")

    ;

    public final String name;

    FileProcessorAction(String name) {
        this.name = name;
    }
}
