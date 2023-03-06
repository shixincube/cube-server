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

package cube.service.fileprocessor.processor.video;

import cell.util.log.Logger;
import cube.common.entity.FileResult;
import cube.file.misc.MediaAttribute;
import cube.file.operation.ExtractAudioOperation;
import cube.service.fileprocessor.processor.ProcessorContext;
import org.json.JSONObject;

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

        extractAudioContext.setVideoOperation(this.operation);

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
        params.add("-vn");
        switch (this.operation.outputType) {
            case MP3:
                // -b:a 192k -ar 44100 -ac 2 -acodec libmp3lame
                params.add("-acodec");
                params.add("libmp3lame");
                break;
            case WAV:
                params.add("-acodec");
                params.add("pcm_s16le");
                break;
            default:
                params.add("-acodec");
                params.add("copy");
                break;
        }
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

        // 读取文件属性
        JSONObject attrJson = this.probe(file.getName());
        if (null != attrJson) {
            try {
                MediaAttribute attribute = new MediaAttribute(attrJson);
                result.mediaAttribute = attribute;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        context.addResult(result);

        if (this.deleteSourceFile) {
            this.inputFile.delete();
        }
    }
}
