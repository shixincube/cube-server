/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
