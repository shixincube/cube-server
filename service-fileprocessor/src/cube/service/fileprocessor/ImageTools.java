/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

package cube.service.fileprocessor;

import cell.util.log.Logger;
import cube.common.entity.Image;
import cube.util.FileType;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 图片工具。
 */
public final class ImageTools {

    public static String WORKING_PATH = "storage/tmp/";

    private ImageTools() {
    }

    /**
     * 识别图片。
     *
     * @param fullpath
     * @return 不支持的文件类型返回 {@code null} 值。
     */
    public static Image identify(String fullpath) {
        ProcessBuilder pb = new ProcessBuilder("identify", "-format", "%m %W %H ", fullpath);

        Process process = null;
        int status = 0;

        Image image = null;

        try {
            String line = null;
            process = pb.start();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = stdInput.readLine()) != null) {
                if (line.length() > 0) {
                    String[] tmp = line.split(" ");
                    if (tmp.length == 3 || line.startsWith("GIF")) {
                        FileType type = FileType.matchExtension(tmp[0]);
                        if (type == FileType.JPEG || type == FileType.PNG || type == FileType.GIF || type == FileType.BMP) {
                            image = new Image(type, Integer.parseInt(tmp[1]), Integer.parseInt(tmp[2]));
                        }
                    }
                }
            }
            while ((line = stdError.readLine()) != null) {
                if (line.length() > 0) {
                    Logger.w(ImageTools.class, "#identify - " + line);
                }
            }

            try {
                status = process.waitFor();
            } catch (InterruptedException e) {
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != process) {
                process.destroy();
            }
        }

        return image;
    }

    /**
     * 生成缩略图。
     *
     * @param inputFile
     * @param outputFile
     * @return
     */
    public static Image thumbnail(String inputFile, String outputFile, int size) {
        ProcessBuilder pb = new ProcessBuilder("convert", inputFile, "-thumbnail", size + "x" + size, outputFile + ".jpg");

        Process process = null;
        int status = 0;

        try {
            String line = null;
            process = pb.start();
//            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
//            while ((line = stdInput.readLine()) != null) {
//            }
            while ((line = stdError.readLine()) != null) {
                if (line.length() > 0) {
                    Logger.w(ImageTools.class, "#identify - " + line);
                }
            }

            try {
                status = process.waitFor();
            } catch (InterruptedException e) {
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != process) {
                process.destroy();
            }
        }

        File file = new File(outputFile + ".jpg");
        if (file.exists()) {
            return ImageTools.identify(file.getAbsolutePath());
        }
        else {
            return null;
        }
    }

    public static void main(String[] args) {
//        Image image = ImageTools.identify("/Users/ambrose/Documents/Repositories/Cube3/cube-server/README.md");
//        System.out.println(image);
//
//        image = ImageTools.identify("/Users/ambrose/Documents/Repositories/Cube3/assets/illustrations/screenshot_classroom_1.jpg");
//        System.out.println(image);
//
//        image = ImageTools.identify("/Users/ambrose/Documents/Repositories/Cube3/assets/illustrations/tutorial_facemonitor_1.png");
//        System.out.println(image);
//
//        image = ImageTools.identify("/Users/ambrose/Documents/Repositories/Cube3/assets/showcase/cloud_file.gif");
//        System.out.println(image);

        Image image = ImageTools.thumbnail("/Users/ambrose/Documents/Repositories/Cube3/assets/illustrations/Cube3Framework.png",
                "service/storage/tmp/t1", 480);
        System.out.println(image);
    }
}
