/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.listener;

import cube.common.entity.FileLabel;
import cube.common.entity.SpeechEmotion;
import cube.common.state.AIGCStateCode;

/**
 * 物体检测监听器。
 */
public interface SpeechEmotionRecognitionListener {

    void onCompleted(FileLabel input, SpeechEmotion result);

    void onFailed(FileLabel input, AIGCStateCode stateCode);
}
