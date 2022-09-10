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
import cube.common.entity.FileResult;
import cube.file.operation.ExtractAudioOperation;
import cube.service.fileprocessor.processor.ProcessorContext;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * 提取音频处理器。
 */
public class ExtractAudioProcessor extends VideoProcessor {

    private ExtractAudioOperation operation;

    public ExtractAudioProcessor(Path workPath, ExtractAudioOperation operation) {
        super(workPath);
        this.operation = operation;
    }

    @Override
    public void go(ProcessorContext context) {
        ExtractAudioContext extractAudioContext = (ExtractAudioContext) context;

        long time = System.currentTimeMillis();

        String outputName = this.getFilename() + "." + this.operation.outputType.getPreferredExtension();

        ArrayList<String> params = new ArrayList<>();
        params.add("-i");
        params.add(this.inputFile.getName());
        params.add("-vn");
        params.add("-acodec");
        params.add("copy");
        params.add(outputName);

        boolean result = this.call(params, extractAudioContext);
        if (!result) {
            Logger.w(this.getClass(), "#go - ffmpeg command failed");
            return;
        }

        extractAudioContext.setElapsedTime(System.currentTimeMillis() - time);
        extractAudioContext.setSuccessful(true);

        // 后处理
        File file = new File(getWorkPath().toFile(), outputName);
        this.postHandle(file, extractAudioContext);
    }

    private void postHandle(File file, ExtractAudioContext context) {
        FileResult result = new FileResult(file);
        context.addResult(result);
    }
}
