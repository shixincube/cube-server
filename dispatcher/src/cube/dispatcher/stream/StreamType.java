/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.stream;

public enum StreamType {

    /**
     * 语音分割及分析。
     */
    SpeakerDiarization("SpeakerDiarization"),

    /**
     * 语音识别。
     */
    //SpeechRecognition("SpeechRecognition")

    ;

    public final String name;

    StreamType(String name) {
        this.name = name;
    }

    public static StreamType parse(String name) {
        for (StreamType type : StreamType.values()) {
            if (type.name.equalsIgnoreCase(name) || type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
