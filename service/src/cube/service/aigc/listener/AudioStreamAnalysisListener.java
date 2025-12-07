/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.listener;

import cube.common.entity.AudioStreamSink;
import cube.common.entity.FileLabel;
import cube.common.state.AIGCStateCode;

/**
 * 自动语音识别监听器。
 */
public interface AudioStreamAnalysisListener {

    void onCompleted(FileLabel source, AudioStreamSink streamSink);

    void onFailed(FileLabel source, AIGCStateCode stateCode);
}
