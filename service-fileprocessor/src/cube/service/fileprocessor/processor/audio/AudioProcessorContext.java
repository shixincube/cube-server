/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.fileprocessor.processor.audio;

import cube.common.action.FileProcessorAction;
import cube.file.AudioOperation;
import cube.processor.ProcessorContext;
import org.json.JSONObject;

import java.io.File;

/**
 * 音频处理器上下文。
 */
public abstract class AudioProcessorContext extends ProcessorContext {

    private AudioOperation audioOperation;

    private File inputFile;

    private long elapsedTime;

    public AudioProcessorContext() {
    }

    public AudioProcessorContext(JSONObject json) {
        super(json);
        this.elapsedTime = json.getLong("elapsed");
    }

    public void setInputFile(File file) {
        this.inputFile = file;
    }

    public File getInputFile() {
        return this.inputFile;
    }

    public void setAudioOperation(AudioOperation value) {
        this.audioOperation = value;
    }

    public AudioOperation getAudioOperation() {
        return this.audioOperation;
    }

    public void setElapsedTime(long value) {
        this.elapsedTime = value;
    }

    public long getElapsedTime() {
        return this.elapsedTime;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON(FileProcessorAction.Audio.name);
        json.put("operation", this.audioOperation.toJSON());
        json.put("elapsed", this.elapsedTime);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
