/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
