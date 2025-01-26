/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.file;

import cube.common.JSONable;
import cube.common.action.FileProcessorAction;
import org.json.JSONObject;

import java.io.File;

/**
 * 音频操作。
 */
public abstract class AudioOperation implements FileOperation, JSONable {

    private File inputFile;

    public AudioOperation() {
    }

    /**
     * 获取具体的音频操作。
     *
     * @return
     */
    public abstract String getOperation();

    @Override
    public String getProcessAction() {
        return FileProcessorAction.Audio.name;
    }

    public File getInputFile() {
        return this.inputFile;
    }

    public void setInputFile(File inputFile) {
        this.inputFile = inputFile;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("process", this.getProcessAction());
        json.put("operation", this.getOperation());
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
