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
import cube.util.TextUtils;

/**
 * 常量数据。
 */
public final class Consts {

    public final static String ANSWER_NO_CONTENT = "很抱歉，我目前没有符合该描述的信息提供给您。";

    private final static String ANSWER_URL_FAILURE_FORMAT = "这是站点 %s 的访问链接：%s\n我没有访问到该链接数据，此链接可能已经失效了。";

    private final static String ANSWER_URL_PAGE_FORMAT = "这是来自站点 %s 的页面链接。\n页面标题是：《 **%s** 》。\n页面正文约 %d 个字符。";

    private final static String ANSWER_URL_IMAGE_FORMAT = "这是张来自 %s 的 %s 图片。\n图片宽 %d 像素，高 %d 像素，文件大小是 %s 。";

    private final static String ANSWER_URL_PLAIN_FORMAT = "这是来自 %s 的文本格式的文件。\n文件包含 %d 个字符，文件大小是 %s 。";

    private final static String ANSWER_URL_VIDEO_FORMAT = "这是来自 %s 的视频文件。\n文件大小是 %s 。";

    private final static String ANSWER_URL_AUDIO_FORMAT = "这是来自 %s 的音频文件。\n文件大小是 %s 。";

    private final static String ANSWER_URL_OTHER_FORMAT = "这是来自 %s 的文件。\n文件大小是 %s 。";

    private final static String ANSWER_URL_SOME_FORMAT = "这是 **%d** 个链接地址，我帮您进行了整理：";

    private final static String ANSWER_URL_SOME_PAGE_FORMAT = "[%s](%s) 是**页面链接** 。";
    private final static String ANSWER_URL_SOME_IMAGE_FORMAT = "[%s](%s) 是**图片链接** 。";
    private final static String ANSWER_URL_SOME_PLAIN_FORMAT = "[%s](%s) 是**文本格式数据链接** 。";
    private final static String ANSWER_URL_SOME_VIDEO_FORMAT = "[%s](%s) 是**视频数据链接** 。";
    private final static String ANSWER_URL_SOME_AUDIO_FORMAT = "[%s](%s) 是**音频数据链接** 。";
    private final static String ANSWER_URL_SOME_OTHER_FORMAT = "[%s](%s) 链接的数据格式是 %s 。";
    private final static String ANSWER_URL_SOME_FAILURE_FORMAT = "链接 [%s](%s) 无法访问。";

    private final static String ANSWER_CHART_FORMAT = "这是**%s**的图表。";

    private final static String ANSWER_CHART_SOME_FORMAT = "目前可以为您提供 **%d** 张图表。";
    private final static String ANSWER_CHART_SOME_ONE_FORMAT = "**%s** 图表。";

    private final static int URL_ELLIPSIS_LIMIT = 56;

    private final static String ASK_FORMAT = "已知信息：%s\n\n" +
            "根据上述已知信息，简洁和专业的来回答用户的问题。如果无法从中得到答案，请说 “根据已知信息无法回答该问题” 或 “没有提供足够的相关信息”，不允许在答案中添加编造成分，答案请使用中文。 问题是：%s";


    public final static String ANSWER_FIND_SOME_YEAR_DATA = "没有找到%s的数据，但是找到了%s的数据。";

    public static String formatUrlFailureAnswer(String domain, String url) {
        return String.format(ANSWER_URL_FAILURE_FORMAT, domain, url);
    }

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

    public static String formatUrlSomeAnswer(int numURLs) {
        return String.format(ANSWER_URL_SOME_FORMAT, numURLs);
    }

    public static String formatUrlSomePageAnswer(String url) {
        return String.format(ANSWER_URL_SOME_PAGE_FORMAT, TextUtils.ellipsisURL(url, URL_ELLIPSIS_LIMIT), url);
    }

    public static String formatUrlSomeImageAnswer(String url) {
        return String.format(ANSWER_URL_SOME_IMAGE_FORMAT, TextUtils.ellipsisURL(url, URL_ELLIPSIS_LIMIT), url);
    }

    public static String formatUrlSomePlainAnswer(String url) {
        return String.format(ANSWER_URL_SOME_PLAIN_FORMAT, TextUtils.ellipsisURL(url, URL_ELLIPSIS_LIMIT), url);
    }

    public static String formatUrlSomeVideoAnswer(String url) {
        return String.format(ANSWER_URL_SOME_VIDEO_FORMAT, TextUtils.ellipsisURL(url, URL_ELLIPSIS_LIMIT), url);
    }

    public static String formatUrlSomeAudioAnswer(String url) {
        return String.format(ANSWER_URL_SOME_AUDIO_FORMAT, TextUtils.ellipsisURL(url, URL_ELLIPSIS_LIMIT), url);
    }

    public static String formatUrlSomeOtherAnswer(String url, String format) {
        return String.format(ANSWER_URL_SOME_OTHER_FORMAT, TextUtils.ellipsisURL(url, URL_ELLIPSIS_LIMIT), url, format);
    }

    public static String formatUrlSomeFailureAnswer(String url) {
        return String.format(ANSWER_URL_SOME_FAILURE_FORMAT, TextUtils.ellipsisURL(url, URL_ELLIPSIS_LIMIT), url);
    }

    public static String formatChartAnswer(String desc) {
        return String.format(ANSWER_CHART_FORMAT, desc);
    }

    public static String formatChartSomeAnswer(int num) {
        return String.format(ANSWER_CHART_SOME_FORMAT, num);
    }

    public static String formatChartSomeOneAnswer(String desc) {
        return String.format(ANSWER_CHART_SOME_ONE_FORMAT, desc);
    }

    public static String formatQuestion(String data, String query) {
        return String.format(ASK_FORMAT, data, query);
    }
}
