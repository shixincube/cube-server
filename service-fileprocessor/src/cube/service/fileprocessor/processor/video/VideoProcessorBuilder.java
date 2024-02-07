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

package cube.service.fileprocessor.processor.video;

import cube.file.operation.ExtractAudioOperation;
import cube.file.operation.SnapshotOperation;
import org.json.JSONObject;

import java.nio.file.Path;

/**
 * 视频处理器构建器。
 */
public final class VideoProcessorBuilder {

    private VideoProcessorBuilder() {
    }

    public static VideoProcessor build(Path workPath, JSONObject operationJson) {
        VideoProcessor processor = null;

        // 解析 Operation
        String operation = operationJson.getString("operation");

        if (SnapshotOperation.Operation.equals(operation)) {
            SnapshotOperation snapshotOperation = new SnapshotOperation(operationJson);
            SnapshotProcessor snapshot = new SnapshotProcessor(workPath, snapshotOperation);
            processor = snapshot;
        }
        else if (ExtractAudioOperation.Operation.equals(operation)) {
            ExtractAudioOperation extractAudioOperation = new ExtractAudioOperation(operationJson);
            ExtractAudioProcessor extractAudio = new ExtractAudioProcessor(workPath, extractAudioOperation);
            processor = extractAudio;
        }

        return processor;
    }
}
