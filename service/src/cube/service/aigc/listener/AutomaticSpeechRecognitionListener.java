/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.listener;

import cube.common.entity.FileLabel;
import cube.common.entity.SpeechRecognitionInfo;
import cube.common.state.AIGCStateCode;

/**
 * 自动语音识别监听器。
 */
public interface AutomaticSpeechRecognitionListener {

    void onCompleted(FileLabel source, SpeechRecognitionInfo result);

    void onFailed(FileLabel source, AIGCStateCode stateCode);
}
