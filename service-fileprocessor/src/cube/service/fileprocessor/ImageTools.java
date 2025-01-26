/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.fileprocessor;

import cell.util.log.Logger;
import cube.common.entity.Image;
import cube.util.FileType;
import cube.util.FileUtils;
import cube.vision.Size;
import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * 图片工具。
 */
public final class ImageTools {

    public static String WORKING_PATH = "storage/tmp/";

    private static boolean USE_IMAGEMAGICK = true;

    private ImageTools() {
    }

    public static boolean check() {
        ProcessBuilder pb = new ProcessBuilder("convert", "-help");

        Process process = null;
        int status = -1;

        try {
            process = pb.start();
            try {
                status = process.waitFor();
            } catch (InterruptedException e) {
            }
        } catch (IOException e) {
            status = 2;
        } finally {
            if (null != process) {
                process.destroy();
            }
        }

        if (status == 1 || status == 0) {
            USE_IMAGEMAGICK = true;
            Logger.i(ImageTools.class, "Use ImageMagick for processing image data");
        }
        else {
            USE_IMAGEMAGICK = false;
            Logger.i(ImageTools.class, "Use Java Image IO for processing image data");
        }

        return USE_IMAGEMAGICK;
    }

    /**
     * 识别图片。
     *
     * @param fullpath
     * @return 不支持的文件类型返回 {@code null} 值。
     */
    public static Image identify(String fullpath) {
        Image image = null;

        if (USE_IMAGEMAGICK) {
            ProcessBuilder pb = new ProcessBuilder("identify", "-format", "%m %w %h ", fullpath);

            Process process = null;
            BufferedReader stdInput = null;
            int status = -1;

            try {
                // 启动进程
                process = pb.start();

                stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line = null;
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

                try {
                    status = process.waitFor();
                } catch (InterruptedException e) {
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (null != stdInput) {
                    try {
                        stdInput.close();
                    } catch (IOException e) {
                    }
                }

                if (null != process) {
                    process.destroy();
                }
            }
        }
        else {
            FileType fileType = FileUtils.extractFileExtensionType(fullpath);
            try {
               BufferedImage source = ImageIO.read(new File(fullpath));
               image = new Image(fileType, source.getWidth(), source.getHeight());
               source = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return image;
    }

    /**
     * 生成缩略图。
     *
     * @param inputFile
     * @param inputImageSize
     * @param outputFile
     * @return
     */
    public static Image thumbnail(String inputFile, Size inputImageSize, String outputFile, int quality) {
        if (USE_IMAGEMAGICK) {
            // 创建命令
            ArrayList<String> command = new ArrayList<>();
            command.add("convert");
            command.add(inputFile);

            if (inputImageSize.width > 1200 || inputImageSize.height > 1200) {
                command.add("-sample");
                command.add("1200");
                command.add("-quality");
                command.add("90");
            }
            else {
                command.add("-quality");
                command.add(Integer.toString(quality));
            }

            command.add(outputFile + ".jpg");

            ProcessBuilder pb = new ProcessBuilder(command);

            Process process = null;
            BufferedReader stdError = null;
            int status = -1;

            try {
                String line = null;
                process = pb.start();
                stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                while ((line = stdError.readLine()) != null) {
                    if (line.length() > 0) {
                        Logger.w(ImageTools.class, "#thumbnail - " + line);
                    }
                }

                try {
                    status = process.waitFor();
                } catch (InterruptedException e) {
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (null != stdError) {
                    try {
                        stdError.close();
                    } catch (IOException e) {
                    }
                }

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
        else {
            int size = inputImageSize.width;
            if (inputImageSize.width > 1000 || inputImageSize.height > 1000) {
                size = 1000;
            }

            try {
                Thumbnails.of(new File(inputFile))
                        .outputQuality(((float)quality) / 100.0f)
                        .size(size, size)
                        .toFile(outputFile + ".jpg");
            } catch (IOException e) {
                e.printStackTrace();
            }

            File file = new File(outputFile + ".jpg");
            if (file.exists()) {
                return ImageTools.identify(file.getAbsolutePath());
            }
            else {
                return null;
            }
        }
    }

    /**
     * 生成缩略图。
     *
     * @param inputFile
     * @param outputFile
     * @param size
     * @return
     */
    public static Image thumbnailResize(String inputFile, String outputFile, int size) {
        if (USE_IMAGEMAGICK) {
            ProcessBuilder pb = new ProcessBuilder("convert", inputFile, "-thumbnail", size + "x" + size, outputFile + ".jpg");

            Process process = null;
            BufferedReader stdError = null;
            int status = 1;

            try {
                String line = null;
                process = pb.start();
                stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                while ((line = stdError.readLine()) != null) {
                    if (line.length() > 0) {
                        Logger.w(ImageTools.class, "#thumbnailResize - " + line);
                    }
                }

                try {
                    status = process.waitFor();
                } catch (InterruptedException e) {
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (null != stdError) {
                    try {
                        stdError.close();
                    } catch (IOException e) {
                    }
                }

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
        else {
            try {
                Thumbnails.of(new File(inputFile))
                        .size(size, size)
                        .toFile(outputFile + ".jpg");
            } catch (IOException e) {
                e.printStackTrace();
            }

            File file = new File(outputFile + ".jpg");
            if (file.exists()) {
                return ImageTools.identify(file.getAbsolutePath());
            }
            else {
                return null;
            }
        }
    }

//    public static void main(String[] args) {
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

//        Image image = ImageTools.thumbnail("/Users/ambrose/Documents/Repositories/Cube3/assets/illustrations/Cube3Framework.png",
//                "service/storage/tmp/t1", 480);
//        System.out.println(image);

//        ImageTools.check();
//    }
}
