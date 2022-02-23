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

import cube.vision.Color;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageMagick {

    private ImageMagick() {
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
