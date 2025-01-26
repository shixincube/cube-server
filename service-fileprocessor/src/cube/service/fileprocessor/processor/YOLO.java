/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.fileprocessor.processor;

import cube.file.YOLOResultFile;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * YOLO 处理器。
 */
public class YOLO extends Processor {

    public final static String TASK_DETECT = "detect";
    public final static String TASK_CLASSIFY = "classify";
    public final static String TASK_SEGMENT = "segment";

    public final static String MODE_TRAIN = "train";
    public final static String MODE_PREDICT = "predict";
    public final static String MODE_VAL = "val";
    public final static String MODE_EXPORT = "export";

    public final static String MODEL_V8_TINY = "yolov8n.pt";
    public final static String MODEL_V8_SMALL = "yolov8s.pt";
    public final static String MODEL_V8_MIDDLE = "yolov8m.pt";
    public final static String MODEL_V8_LARGE = "yolov8l.pt";
    public final static String MODEL_V8_EXTRA_LARGE = "yolov8x.pt";

    public YOLO(Path workPath) {
        super(workPath);
    }

    public YOLOResultFile predict(File file) {
        List<String> commandLine = new ArrayList<>();
        commandLine.add("yolo");
        commandLine.add(TASK_DETECT);
        commandLine.add(MODE_PREDICT);
        commandLine.add("model=" + MODEL_V8_SMALL);
        commandLine.add("source=\"" + file.getAbsolutePath() + "\"");
        commandLine.add("save=True");
        commandLine.add("save_crop=True");

        Process process = null;
        ProcessBuilder pb = new ProcessBuilder(commandLine);
        // 设置工作目录
        pb.directory(getWorkPath().toFile());



        return null;
    }

    @Override
    public void go(ProcessorContext context) {

    }
}
