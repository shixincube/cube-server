/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

package cube.service.fileprocessor.processor;

import cube.common.entity.TextConstraint;
import cube.util.FileUtils;
import cube.vision.Color;
import cube.vision.Rectangle;
import cube.vision.Size;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class ImageMagick {

    private ImageMagick() {
    }

    /**
     * 剪裁指定区域图像。
     *
     * @param workPath
     * @param filename
     * @param output
     * @param rect
     * @return
     */
    public static boolean crop(File workPath, String filename, String output, Rectangle rect) {
        List<String> commandLine = new ArrayList<>();
        commandLine.add("convert");
        commandLine.add(filename);
        commandLine.add("-crop");
        commandLine.add(rect.toString());
        commandLine.add(output);

        int status = execute(workPath, commandLine);
        if (0 == status || 1 == status) {
            File file = new File(workPath, output);
            return file.exists();
        }
        else {
            return false;
        }
    }

    /**
     * 剔除颜色。
     *
     * @param workPath
     * @param filename
     * @param output
     * @param reserved
     * @param fill
     */
    public static boolean eliminateColor(File workPath, String filename, String output, Color reserved, Color fill) {
        List<String> commandLine = new ArrayList<>();
        commandLine.add("convert");
        commandLine.add(filename);
        commandLine.add("-fill");
        commandLine.add(fill.formatHex());
        commandLine.add("-fuzz");
        commandLine.add("15%");
        commandLine.add("+opaque");
        commandLine.add(reserved.formatHex());
        commandLine.add(output);

        int status = execute(workPath, commandLine);
        if (0 == status || 1 == status) {
            File file = new File(workPath, output);
            return file.exists();
        }
        else {
            return false;
        }
    }

    /**
     * 反转图像颜色。
     *
     * @param workPath
     * @param filename
     * @param output
     * @return
     */
    public static boolean reverseColor(File workPath, String filename, String output) {
        List<String> commandLine = new ArrayList<>();
        commandLine.add("convert");
        commandLine.add(filename);
        commandLine.add("-bias");
        commandLine.add("50%");
        commandLine.add("-channel");
        commandLine.add("RGB");
        commandLine.add("-negate");
        commandLine.add(output);

        int status = execute(workPath, commandLine);
        if (0 == status || 1 == status) {
            File file = new File(workPath, output);
            return file.exists();
        }
        else {
            return false;
        }
    }

    /**
     * 替换指定颜色。
     *
     * @param workPath
     * @param filename
     * @param output
     * @param targetColor
     * @param replaceColor
     * @param fuzz
     * @return
     */
    public static boolean replaceColor(File workPath, String filename, String output,
                                       Color targetColor, Color replaceColor, int fuzz) {
        List<String> commandLine = new ArrayList<>();
        commandLine.add("convert");
        commandLine.add(filename);
        commandLine.add("-channel");
        commandLine.add("rgba");
        commandLine.add("-fuzz");
        commandLine.add(fuzz + "%");
        commandLine.add("-fill");
        commandLine.add(replaceColor.formatRGB());
        commandLine.add("-opaque");
        commandLine.add(targetColor.formatRGB());
        commandLine.add(output);

        int status = execute(workPath, commandLine);
        if (0 == status || 1 == status) {
            File file = new File(workPath, output);
            return file.exists();
        }
        else {
            return false;
        }
    }

    /**
     * 图像转灰度图。
     *
     * @param workPath
     * @param filename
     * @param output
     * @return
     */
    public static boolean grayscale(File workPath, String filename, String output) {
        List<String> commandLine = new ArrayList<>();
        commandLine.add("convert");
        commandLine.add(filename);
        commandLine.add("-colorspace");
        commandLine.add("Gray");
        commandLine.add(output);

        int status = execute(workPath, commandLine);
        if (0 == status || 1 == status) {
            File file = new File(workPath, output);
            return file.exists();
        }
        else {
            return false;
        }
    }

    /**
     * 调整图片亮度和对比度。
     *
     * @param workPath
     * @param filename
     * @param output
     * @param brightness
     * @param contrast
     * @return
     */
    public static boolean brightness(File workPath, String filename, String output, int brightness, int contrast) {
        List<String> commandLine = new ArrayList<>();
        commandLine.add("convert");
        commandLine.add(filename);
        commandLine.add("-brightness-contrast");
        commandLine.add(brightness + "x" + contrast);
        commandLine.add(output);

        int status = execute(workPath, commandLine);
        if (0 == status || 1 == status) {
            File file = new File(workPath, output);
            return file.exists();
        }
        else {
            return false;
        }
    }

    /**
     * 将指定文本隐写到图片内。
     *
     * @param workPath
     * @param hiddenText
     * @param markSize
     * @param textConstraint
     * @param imageFile
     * @param output
     * @return
     */
    public static boolean steganography(File workPath, String hiddenText, Size markSize, TextConstraint textConstraint,
                                        String imageFile, String output) {
        String watermark = "watermark_" + FileUtils.extractFileName(output) + ".gif";

        List<String> commandLine = new ArrayList<>();
        commandLine.add("convert");
        commandLine.add("-gravity");
        commandLine.add("center");
        commandLine.add("-size");
        commandLine.add(markSize.width + "x" + markSize.height);
        commandLine.add("-font");
        commandLine.add(null == textConstraint.font ?
                Paths.get("").toAbsolutePath().toString() + "/assets/STHeiti.ttc" : textConstraint.font);
        commandLine.add("-pointsize");
        commandLine.add(Integer.toString(textConstraint.pointSize));

        // 文本间距控制
        commandLine.add("-kerning");
        commandLine.add("2");
        commandLine.add("-interline-spacing");
        commandLine.add("10");

        commandLine.add("-fill");
        commandLine.add(textConstraint.color.formatHex());
        commandLine.add("label:" + hiddenText);
        commandLine.add(watermark);

        File watermarkFile = new File(workPath, watermark);
        int status = execute(workPath, commandLine);
        if (0 == status || 1 == status) {
            if (!watermarkFile.exists()) {
                return false;
            }
        }
        else {
            return false;
        }

        commandLine.clear();
        // 将水印隐写到图片
        commandLine.add("composite");
        commandLine.add(watermark);
        commandLine.add(imageFile);
        commandLine.add("-stegano");
        commandLine.add("+0+0");
        commandLine.add(output);

        status = execute(workPath, commandLine);

        // 删除水印文件。
        watermarkFile.delete();

        if (0 == status || 1 == status) {
            File file = new File(workPath, output);
            return file.exists();
        }
        else {
            return false;
        }
    }

    /**
     * 恢复隐写的水印。
     *
     * @param workPath
     * @param markSize
     * @param imageFile
     * @param output
     * @return
     */
    public static boolean recoverSteganography(File workPath, Size markSize, String imageFile, String output) {
        List<String> commandLine = new ArrayList<>();
        commandLine.add("convert");
        commandLine.add("-size");
        commandLine.add(markSize.width + "x" + markSize.height + "+0+0");
        commandLine.add("stegano:" + imageFile);
        commandLine.add(output);

        int status = execute(workPath, commandLine);
        if (0 == status || 1 == status) {
            File file = new File(workPath, output);
            return file.exists();
        }
        else {
            return false;
        }
    }

    private static int execute(File workPath, List<String> commandLine) {
        int status = -1;

        Process process = null;
        ProcessBuilder pb = new ProcessBuilder(commandLine);
        // 设置工作目录
        pb.directory(workPath);

        try {
            process = pb.start();

            try {
                status = process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != process) {
                process.destroy();
            }

            process = null;
        }

        return status;
    }
}
