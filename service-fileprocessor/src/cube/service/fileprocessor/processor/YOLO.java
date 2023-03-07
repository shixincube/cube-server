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
