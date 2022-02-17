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

import cell.util.log.Logger;
import cube.common.entity.FileLabel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * 视频帧处理器。
 */
public class SnapshotFrameProcessor extends FFmpeg {

    private File inputVideoFile;

    private FileLabel videoFileLabel;

    public SnapshotFrameProcessor(Path workPath) {
        super(workPath);
    }

    public void setInputVideoFile(File inputVideoFile, FileLabel fileLabel) {
        this.inputVideoFile = inputVideoFile;
        this.videoFileLabel = fileLabel;
    }

    @Override
    public void go(ProcessorContext context) {
        if (!(context instanceof SnapshotContext)) {
            return;
        }

        SnapshotContext snapshotContext = (SnapshotContext) context;

        // 创建输出目录
        Path outputPath = Paths.get(getWorkPath().toString(), this.videoFileLabel.getFileCode());
        if (!Files.exists(outputPath)) {
            try {
                Files.createDirectories(outputPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        snapshotContext.videoFile = this.videoFileLabel;

        ArrayList<String> params = new ArrayList<>();
        params.add("-ss");
        params.add(snapshotContext.startTime.formatHMS());
        params.add("-i");
        params.add(this.inputVideoFile.getName());
        params.add("-f");
        params.add("image2");
        params.add("-r");
        params.add(String.format("%.2f", snapshotContext.rate));
        params.add("-y");
        params.add(this.videoFileLabel.getFileCode() + "/%04d." + snapshotContext.outputType.getPreferredExtension());

        boolean result = this.call(params, context);
        if (!result) {
            Logger.w(this.getClass(), "#go - ffmpeg command failed");
            return;
        }

        context.setSuccessful(true);
    }
}
