/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

import cube.file.AudioOperation;
import cube.file.operation.AudioCropOperation;
import cube.file.operation.AudioSamplingOperation;
import org.json.JSONObject;

import java.nio.file.Path;

/**
 * 音频处理器构建器。
 */
public final class AudioProcessorBuilder {

    private AudioProcessorBuilder() {
    }

    public static AudioProcessor build(Path workPath, JSONObject operationJson) {
        AudioProcessor processor = null;

        // 解析 Operation
        String operation = operationJson.getString("operation");

        if (AudioCropOperation.Operation.equals(operation)) {
            AudioCropOperation cropOperation = new AudioCropOperation(operationJson);
            AudioCropProcessor crop = new AudioCropProcessor(workPath, cropOperation);
            processor = crop;
        }
        else if (AudioSamplingOperation.Operation.equals(operation)) {
            AudioSamplingOperation samplingOperation = new AudioSamplingOperation(operationJson);
            AudioSamplingProcessor sampling = new AudioSamplingProcessor(workPath, samplingOperation);
            processor = sampling;
        }

        return processor;
    }
}
