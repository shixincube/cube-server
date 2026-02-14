/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.processor;

import cell.util.log.Logger;
import cube.util.ConfigUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FFMPEG 工具。
 */
public abstract class FFmpeg extends Processor {

    private AtomicBoolean running;

    private String ffmpegCommand;
    private String ffprobeCommand;

    public FFmpeg(Path workPath) {
        super(workPath);
        this.running = new AtomicBoolean(false);
        this.ffmpegCommand = "/usr/bin/ffmpeg";
        this.ffprobeCommand = "/usr/bin/ffprobe";

        try {
            File config = new File("config/ffmpeg.properties");
            if (!config.exists()) {
                config = new File("ffmpeg.properties");
            }

            if (config.exists()) {
                Properties properties = ConfigUtils.readProperties(config.getAbsolutePath());
                this.ffmpegCommand = properties.getProperty("ffmpeg", "/usr/bin/ffmpeg");
                this.ffprobeCommand = properties.getProperty("ffprobe", "/usr/bin/ffprobe");
            }
            else {
                Logger.w(getClass(), "Can NOT find ffmpeg config properties file, use default config: \"" +
                        this.ffmpegCommand + "\" and \"" + this.ffprobeCommand + "\"");
            }
        } catch (IOException e) {
            Logger.w(getClass(), "#FFmpeg", e);
        }
    }

    public boolean isRunning() {
        return this.running.get();
    }

    /**
     * 探测媒体文件属性。
     *
     * @param filePath
     * @return
     */
    protected JSONObject probe(String filePath) {
        List<String> commandLine = new ArrayList<>();
        commandLine.add(this.ffprobeCommand);
        commandLine.add("-v");
        commandLine.add("quiet");
        commandLine.add("-show_format");
        commandLine.add("-show_streams");
        commandLine.add("-print_format");
        commandLine.add("json");
        commandLine.add(filePath);

        Process process = null;
        ProcessBuilder pb = new ProcessBuilder(commandLine);
        // 设置工作目录
        pb.directory(getWorkPath().toFile());

        ProcessorContext context = new ProcessorContext() {
            @Override
            public JSONObject toJSON() {
                List<String> lines = getStdOutput();
                if (null != lines) {
                    StringBuilder buf = new StringBuilder();
                    for (String line : lines) {
                        buf.append(line.trim()).append("\n");
                    }

                    return new JSONObject(buf.toString());
                }
                else {
                    return null;
                }
            }

            @Override
            public JSONObject toCompactJSON() {
                return this.toJSON();
            }
        };

        int status = -1;
        try {
            process = pb.start();

            this.running.set(true);

            Runnable worker = this.buildInputStreamWorker(process.getInputStream(), context);
            worker.run();

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

        this.running.set(false);
        return context.toJSON();
    }

    protected boolean call(List<String> params, ProcessorContext context) {
        List<String> commandLine = new ArrayList<>();
        commandLine.add(this.ffmpegCommand);
        commandLine.addAll(params);

        int status = -1;

        Process process = null;
        ProcessBuilder pb = new ProcessBuilder(commandLine);
        // 设置工作目录
        pb.directory(getWorkPath().toFile());

        try {
            process = pb.start();

            this.running.set(true);

            Runnable worker = this.buildInputStreamWorker(process.getErrorStream(), context);
            worker.run();

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

        this.running.set(false);
        return (0 == status || 1 == status);
    }
}
