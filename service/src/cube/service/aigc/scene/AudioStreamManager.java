/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cube.common.entity.AudioStreamSink;

public class AudioStreamManager {

    private final static AudioStreamManager instance = new AudioStreamManager();

    private AudioStreamManager() {
    }

    public static AudioStreamManager getInstance() {
        return AudioStreamManager.instance;
    }

    public void record(AudioStreamSink streamSink) {

    }
}
