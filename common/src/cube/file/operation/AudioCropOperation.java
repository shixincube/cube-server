/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.file.operation;

import cube.file.AudioOperation;
import cube.util.FileType;
import org.json.JSONObject;

/**
 * 音频文件重新采样。
 */
public class AudioCropOperation extends AudioOperation {

    public final static String Operation = "AudioCrop";

    private final static String KEY_OUTPUT_TYPE = "outputType";

    private final static String KEY_START = "start";

    private final static String KEY_TIME = "time";

    public FileType outputType;

    public int start = 0;

    public int time = 30;

    public AudioCropOperation(int start, int time) {
        super();
        this.outputType = FileType.WAV;
        this.start = start;
        this.time = time;
    }

    public AudioCropOperation(int start, int time, FileType outputType) {
        super();
        this.outputType = outputType;
        this.start = start;
        this.time = time;
    }

    public AudioCropOperation(JSONObject json) {
        super();
        this.outputType = FileType.matchExtension(json.getString(KEY_OUTPUT_TYPE));
        this.start = json.getInt(KEY_START);
        this.time = json.getInt(KEY_TIME);
    }

    @Override
    public String getOperation() {
        return AudioCropOperation.Operation;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put(KEY_OUTPUT_TYPE, this.outputType.getPreferredExtension());
        json.put(KEY_START, this.start);
        json.put(KEY_TIME, this.time);
        return json;
    }
}
