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

package cube.service.fileprocessor.processor.video;

import cell.util.log.Logger;
import cube.common.entity.ProcessResultStream;
import cube.file.SnapshotOperation;
import cube.service.fileprocessor.processor.ProcessorContext;
import cube.util.FileUtils;
import cube.util.TimeUtils;
import cube.util.ZipUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * 视频帧处理器。
 */
public class SnapshotProcessor extends VideoProcessor {

    private SnapshotOperation snapshotOperation;

    public SnapshotProcessor(Path workPath, SnapshotOperation operation) {
        super(workPath);
        this.snapshotOperation = operation;
    }

    public SnapshotOperation getSnapshotOperation() {
        return this.snapshotOperation;
    }

    private String getFilename() {
        if (null != this.inputFileLabel) {
            return FileUtils.extractFileName(this.inputFileLabel.getFileName());
        }
        else {
            return FileUtils.extractFileName(this.inputFile.getName());
        }
    }

    private void postHandle(File path, SnapshotContext snapshotContext) {
        if (!path.isDirectory()) {
            return;
        }

        File outputFile = new File(path, "snapshot_" + TimeUtils.formatDateForPathSymbol(System.currentTimeMillis()) + "_"
                        + FileUtils.extractFileName(this.getFilename()) + ".zip");
        if (outputFile.exists()) {
            outputFile.delete();
        }

        File[] files = path.listFiles();
        if (null == files || files.length == 0) {
            return;
        }

        ArrayList<File> list = new ArrayList<>();
        for (File file : files) {
            if (file.isFile() && !file.isHidden()) {
                list.add(file);
            }
        }

        try {
            ZipUtils.toZip(list, new FileOutputStream(outputFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (outputFile.exists()) {
            ProcessResultStream resultStream = new ProcessResultStream(outputFile);
            snapshotContext.setResultStream(resultStream);
        }
    }

    @Override
    public void go(ProcessorContext context) {
        if (!(context instanceof SnapshotContext)) {
            return;
        }

        long time = System.currentTimeMillis();

        this.snapshotOperation.setInputFile(this.inputFile);

        SnapshotContext snapshotContext = (SnapshotContext) context;
        // 设置参数
        snapshotContext.setVideoOperation(this.snapshotOperation);

        String output = (null != this.inputFileLabel) ? this.inputFileLabel.getFileCode() :
                this.getFilename() + "_" + TimeUtils.formatDateForPathSymbol(time);

        // 创建输出目录
        Path outputPath = Paths.get(getWorkPath().toString(), output);
        if (!Files.exists(outputPath)) {
            try {
                Files.createDirectories(outputPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        ArrayList<String> params = new ArrayList<>();
        params.add("-ss");
        params.add(snapshotContext.startTime.formatHMS());
        params.add("-i");
        params.add(this.inputFile.getName());
        params.add("-f");
        params.add("image2");
        params.add("-r");
        params.add(String.format("%.2f", snapshotContext.rate));
        params.add("-y");
        params.add(output + "/%04d." + snapshotContext.outputType.getPreferredExtension());

        boolean result = this.call(params, context);
        if (!result) {
            Logger.w(this.getClass(), "#go - ffmpeg command failed");
            return;
        }

        snapshotContext.setElapsedTime(System.currentTimeMillis() - time);
        snapshotContext.setSuccessful(true);

        // 后处理
        File path = new File(getWorkPath().toFile(), output);
        this.postHandle(path, snapshotContext);
    }
}
