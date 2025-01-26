/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.fileprocessor.processor.video;

import cube.common.action.FileProcessorAction;
import cube.file.VideoOperation;
import cube.service.fileprocessor.processor.ProcessorContext;
import org.json.JSONObject;

import java.io.File;

/**
 * 视频处理器上下文。
 */
public abstract class VideoProcessorContext extends ProcessorContext {

    private VideoOperation videoOperation;

    private File inputFile;

    private long elapsedTime;

    public VideoProcessorContext() {
    }

    public VideoProcessorContext(JSONObject json) {
        super(json);
        this.elapsedTime = json.getLong("elapsed");
    }

    public void setInputFile(File file) {
        this.inputFile = file;
    }

    public File getInputFile() {
        return this.inputFile;
    }

    public void setVideoOperation(VideoOperation value) {
        this.videoOperation = value;
    }

    public VideoOperation getVideoOperation() {
        return this.videoOperation;
    }

    public void setElapsedTime(long value) {
        this.elapsedTime = value;
    }

    public long getElapsedTime() {
        return this.elapsedTime;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON(FileProcessorAction.Video.name);
        json.put("operation", this.videoOperation.toJSON());
        json.put("elapsed", this.elapsedTime);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
