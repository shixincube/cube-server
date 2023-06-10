/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.aigc;

import cube.util.FileUtils;

/**
 * 常量数据。
 */
public final class Consts {

    private final static String ANSWER_URL_PAGE_FORMAT = "这是来自站点 %s 的页面链接。\n页面标题是：《%s》。\n页面正文约 %d 个字符。";

    private final static String ANSWER_URL_IMAGE_FORMAT = "这是张来自 %s 的 %s 图片。\n图片宽 %d 像素，高 %d 像素，文件大小是 %s 。";

    private final static String ANSWER_URL_PLAIN_FORMAT = "这是来自 %s 的文本格式的文件。\n文件包含 %d 个字符，文件大小是 %s 。";

    private final static String ANSWER_URL_VIDEO_FORMAT = "这是来自 %s 的视频文件。\n文件大小是 %s 。";

    private final static String ANSWER_URL_AUDIO_FORMAT = "这是来自 %s 的音频文件。\n文件大小是 %s 。";

    private final static String ANSWER_URL_OTHER_FORMAT = "这是来自 %s 的文件。\n文件大小是 %s 。";

    public static String formatUrlPageAnswer(String domain, String title, int numWords) {
        return String.format(ANSWER_URL_PAGE_FORMAT, domain, title, numWords);
    }

    public static String formatUrlImageAnswer(String domain, String format, int width, int height, long size) {
        return String.format(ANSWER_URL_IMAGE_FORMAT, domain, format, width, height,
                FileUtils.scaleFileSize(size).toString());
    }

    public static String formatUrlPlainAnswer(String domain, int numWords, long size) {
        return String.format(ANSWER_URL_PLAIN_FORMAT, domain, numWords, FileUtils.scaleFileSize(size).toString());
    }

    public static String formatUrlVideoAnswer(String domain, long size) {
        return String.format(ANSWER_URL_VIDEO_FORMAT, domain, FileUtils.scaleFileSize(size).toString());
    }

    public static String formatUrlAudioAnswer(String domain, long size) {
        return String.format(ANSWER_URL_AUDIO_FORMAT, domain, FileUtils.scaleFileSize(size).toString());
    }

    public static String formatUrlOtherAnswer(String domain, long size) {
        return String.format(ANSWER_URL_OTHER_FORMAT, domain, FileUtils.scaleFileSize(size).toString());
    }
}
