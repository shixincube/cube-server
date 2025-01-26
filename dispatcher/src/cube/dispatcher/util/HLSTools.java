/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.util;

import cell.util.log.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * HLS 工具。
 */
public final class HLSTools {

    private static String ffmpeg = null;

    private HLSTools() {
    }

    private static String loadFFmpegPath() {
        File configFile = new File("HLSTools.properties");
        if (!configFile.exists()) {
            configFile = new File("config/HLSTools.properties");
            if (!configFile.exists()) {
                return null;
            }
        }

        String path = null;

        FileInputStream fis = null;

        try {
            fis = new FileInputStream(configFile);
            Properties properties = new Properties();
            properties.load(fis);
            path = properties.getProperty("ffmpeg");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
        }

        if (null != path) {
            ffmpeg = path;
        }

        return path;
    }

    /**
     * 校验工具是否可用。
     *
     * @return
     */
    public static boolean checkEnabled() {
        String ffmpegPath = loadFFmpegPath();
        if (null == ffmpegPath) {
            return false;
        }

        int status = -1;

        Process process = null;
        ProcessBuilder pb = new ProcessBuilder(ffmpegPath, "-version");

        try {
            process = pb.start();

//            FlexibleByteBuffer buf = new FlexibleByteBuffer(512);
//            byte[] bytes = new byte[128];
//            InputStream is = process.getInputStream();
//            int len = 0;
//            while ((len = is.read(bytes)) > 0) {
//                buf.put(bytes, 0, len);
//            }
//            buf.flip();
//            String output = new String(buf.array(), 0, buf.limit());

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
        commandLine.add(null != ffmpeg ? ffmpeg : "ffmpeg");
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
