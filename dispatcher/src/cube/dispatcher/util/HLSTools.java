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

package cube.dispatcher.util;

import cell.util.log.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * HLS 工具。
 */
public final class HLSTools {

    private HLSTools() {
    }

    /**
     * 校验工具是否可用。
     *
     * @return
     */
    public static boolean checkEnabled() {
        List<String> commandLine = new ArrayList<>();
        commandLine.add("ffmpeg");
        commandLine.add("-version");

        int status = -1;

        Process process = null;
        ProcessBuilder pb = new ProcessBuilder(commandLine);

        try {
            process = pb.start();

            try {
                status = process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            Logger.w(HLSTools.class, "#checkEnabled", e);
        } finally {
            if (null != process) {
                process.destroy();
            }

            process = null;
        }

        return (0 == status || 1 == status);
    }

    public static boolean toHLS(File workPath, File inputFile, File outputFile) {
        List<String> commandLine = new ArrayList<>();
        commandLine.add("ffmpeg");
        commandLine.add("-re");
        commandLine.add("-i");
        commandLine.add(inputFile.getName());
        commandLine.add("-c");
        commandLine.add("copy");
        commandLine.add("-f");
        commandLine.add("hls");
        commandLine.add("-hls_time");
        commandLine.add("10");
        commandLine.add("-hls_list_size");
        commandLine.add("0");
        commandLine.add("-bsf:v");
        commandLine.add("h264_mp4toannexb");
        commandLine.add(outputFile.getName());

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
            Logger.w(HLSTools.class, "toHLS", e);
        } finally {
            if (null != process) {
                process.destroy();
            }

            process = null;
        }

        if (0 == status || 1 == status) {
            return true;
        }
        else {
            Logger.w(HLSTools.class, "#toHLS : " + status);
            return false;
        }
    }
}