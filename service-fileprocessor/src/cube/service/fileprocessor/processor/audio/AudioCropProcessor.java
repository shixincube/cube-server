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

import cube.file.operation.AudioCropOperation;
import cube.service.fileprocessor.processor.ProcessorContext;
import org.json.JSONObject;

import java.nio.file.Path;

/**
 * 音频文件裁剪处理器。
 */
public class AudioCropProcessor extends AudioProcessor {

    private AudioCropOperation operation;

    public AudioCropProcessor(Path workPath, AudioCropOperation operation) {
        super(workPath);
        this.operation = operation;
    }

    @Override
    public void go(ProcessorContext context) {
        AudioCropContext cropContext = (AudioCropContext) context;
        cropContext.setAudioOperation(this.operation);

        long time = System.currentTimeMillis();

        JSONObject info = probe(cropContext.getInputFile().getAbsolutePath());

        System.out.println("XJW:\n" + info.toString(4));

        cropContext.setElapsedTime(System.currentTimeMillis() - time);

        if (this.deleteSourceFile) {
            // 删除输入文件
            this.inputFile.delete();
        }
    }
}
