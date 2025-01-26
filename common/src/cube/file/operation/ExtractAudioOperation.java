/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.file.operation;

import cube.file.VideoOperation;
import cube.util.FileType;
import org.json.JSONObject;

/**
 * 提取视频中的音频通道。
 */
public class ExtractAudioOperation extends VideoOperation {

    public final static String Operation = "ExtractAudio";

    private final static String KEY_OUTPUT_TYPE = "outputType";

    private final static String KEY_CHANNEL = "ac";

    private final static String KEY_SAMPLING_RATE = "ar";

    public FileType outputType;

    public ExtractAudioOperation() {
        super();
        this.outputType = FileType.MP3;
    }

    public ExtractAudioOperation(FileType outputType) {
        super();
        this.outputType = outputType;
    }

    public ExtractAudioOperation(JSONObject json) {
        super();
        this.outputType = FileType.matchExtension(json.getString(KEY_OUTPUT_TYPE));
    }

    @Override
    public String getOperation() {
        return ExtractAudioOperation.Operation;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put(KEY_OUTPUT_TYPE, this.outputType.getPreferredExtension());
        return json;
    }
}
