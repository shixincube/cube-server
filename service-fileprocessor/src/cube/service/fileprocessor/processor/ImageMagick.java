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

/**
 * 相关 PDF 操作需要解除限制
 * "/etc/ImageMagick-6/policy.xml"
 * 注释 “disable ghostscript format types” 相关
 */
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
        if (-1 != status) {
            File file = new File(workPath, output);
            return awaits(file);
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
        if (-1 != status) {
            File file = new File(workPath, output);
            return awaits(file);
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
        if (-1 != status) {
            File file = new File(workPath, output);
            return awaits(file);
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
        if (-1 != status) {
            File file = new File(workPath, output);
            return awaits(file);
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
        if (-1 != status) {
            File file = new File(workPath, output);
            return awaits(file);
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
        if (-1 != status) {
            File file = new File(workPath, output);
            return awaits(file);
        }
        else {
            return false;
        }
    }

    /**
     * 图像锐化。
     *
     * @param workPath
     * @param filename
     * @param output
     * @param sigma
     * @return
     */
    public static boolean sharpen(File workPath, String filename, String output, double sigma) {
        List<String> commandLine = new ArrayList<>();
        commandLine.add("convert");
        commandLine.add(filename);
        commandLine.add("-sharpen");
        commandLine.add("0x" + String.format("%.1f", sigma));
        commandLine.add(output);

        int status = execute(workPath, commandLine);
        if (-1 != status) {
            File file = new File(workPath, output);
            return awaits(file);
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
        if (-1 != status) {
            if (!awaits(watermarkFile)) {
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

        if (-1 != status) {
            File file = new File(workPath, output);
            return awaits(file);
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
        if (-1 != status) {
            File file = new File(workPath, output);
            return awaits(file);
        }
        else {
            return false;
        }
    }

    /**
     * 在图片上覆盖贴片水印效果。
     *
     * @param workPath
     * @param text
     * @param textConstraint
     * @param imageFile
     * @param outputFile
     * @return
     */
    public static boolean coverWatermark(File workPath, String text, TextConstraint textConstraint,
                                         String imageFile, String outputFile) {
        List<String> commandLine = new ArrayList<>();
        commandLine.add("convert");
        commandLine.add("-size");
        commandLine.add("200x200");
        commandLine.add("xc:none");
        commandLine.add("-fill");
        commandLine.add("'rgba(" + textConstraint.color.red() + ","
                + textConstraint.color.green() + ","
                + textConstraint.color.blue() + ",0.75)'");
        commandLine.add("-font");
        commandLine.add(null == textConstraint.font ?
               "'" + Paths.get("").toAbsolutePath().toString() + "/assets/STHeiti.ttc'" : textConstraint.font);
        commandLine.add("-pointsize");
        commandLine.add(Integer.toString(textConstraint.pointSize));
        commandLine.add("-gravity");
        commandLine.add("center");
        commandLine.add("-draw");
        commandLine.add("'rotate -35 text 0,0 \"" + text + "\"'");
        commandLine.add("miff:-");
        commandLine.add("|");
        commandLine.add("composite");
        commandLine.add("-tile");
        commandLine.add("-dissolve");
        commandLine.add("40");
        commandLine.add("-");
        commandLine.add(imageFile);
        commandLine.add(outputFile);

        StringBuilder command = new StringBuilder();
        for (String c : commandLine) {
            command.append(c);
            command.append(" ");
        }
        command.deleteCharAt(command.length() - 1);

        commandLine.clear();
        commandLine.add("/bin/bash");
        commandLine.add("-c");
        commandLine.add(command.toString());

        // 执行命令
        execute(workPath, commandLine);
        File file = new File(workPath, outputFile);
        return awaits(file, 3000);
    }

    /**
     * PDF 转 PNG 。
     * @param workPath
     * @param inputFilename
     * @return
     */
    public static List<File> pdf2png(File workPath, String inputFilename) {
        // convert -density 192 file.pdf -quality 100 -alpha remove file.png
        String name = FileUtils.extractFileName(inputFilename);
        String outputFilename = name + ".png";
        List<String> commandLine = new ArrayList<>();
        commandLine.add("convert");
        commandLine.add("-density");
        commandLine.add("192");
        commandLine.add(inputFilename);
        commandLine.add("-quality");
        commandLine.add("100");
        commandLine.add("-alpha");
        commandLine.add("remove");
        commandLine.add(outputFilename);

        int status = execute(workPath, commandLine);
        if (-1 != status) {
            List<File> files = new ArrayList<>();

            File file = new File(workPath, outputFilename);
            if (file.exists()) {
                files.add(file);
            }
            else {
                for (int i = 0; i < 100; ++i) {
                    String filename = name + "-" + i + ".png";
                    file = new File(workPath, filename);
                    if (!file.exists()) {
                        break;
                    }

                    files.add(file);
                }
            }

            return files;
        }
        else {
            return null;
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

    private static boolean awaits(File file) {
        if (!file.exists()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return file.exists();
    }

    private static boolean awaits(File file, long timeout) {
        long start = System.currentTimeMillis();
        while (!file.exists()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (System.currentTimeMillis() - start > timeout) {
                break;
            }
        }
        return file.exists();
    }
}
