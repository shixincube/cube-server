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

package cube.service.fileprocessor.processor.audio;

import cell.util.log.Logger;
import cube.common.entity.FileResult;
import cube.file.operation.AudioSamplingOperation;
import cube.service.fileprocessor.processor.ProcessorContext;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * 音频文件采样处理器。
 */
public class AudioSamplingProcessor extends AudioProcessor {

    private AudioSamplingOperation operation;

    public AudioSamplingProcessor(Path workPath, AudioSamplingOperation operation) {
        super(workPath);
        this.operation = operation;
    }

    @Override
    public void go(ProcessorContext context) {
        AudioSamplingContext samplingContext = (AudioSamplingContext) context;
        samplingContext.setAudioOperation(this.operation);

        long time = System.currentTimeMillis();

        String outputName = this.getFilename() + "_" + System.currentTimeMillis()
                + "." + this.operation.outputType.getPreferredExtension();
        File outputFile = new File(this.getWorkPath().toFile(), outputName);
        if (outputFile.exists()) {
            outputFile.delete();
        }

        ArrayList<String> params = new ArrayList<>();
        params.add("-i");
        params.add(this.inputFile.getName());
        params.add("-ac");
        params.add(Integer.toString(this.operation.channel));
        params.add("-ar");
        params.add(Integer.toString(this.operation.rate));
        params.add(outputName);

        boolean result = this.call(params, samplingContext);
        if (!result) {
            Logger.w(this.getClass(), "#go - ffmpeg command failed");
            return;
        }

        samplingContext.setElapsedTime(System.currentTimeMillis() - time);
        samplingContext.setSuccessful(true);

        FileResult fileResult = new FileResult(outputFile);
        samplingContext.addResult(fileResult);

        if (this.deleteSourceFile) {
            // 删除输入文件
            this.inputFile.delete();
        }
    }
}