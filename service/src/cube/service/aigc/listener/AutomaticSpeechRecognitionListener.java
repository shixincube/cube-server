/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.listener;

import cube.common.entity.ASRResult;
import cube.common.entity.FileLabel;

/**
 * 自动语音识别监听器。
 */
public interface AutomaticSpeechRecognitionListener {

    void onCompleted(FileLabel input, ASRResult result);

    void onFailed(FileLabel source);
}
