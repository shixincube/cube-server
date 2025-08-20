/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.listener;

import cube.common.entity.FileLabel;
import cube.common.entity.VoiceDiarization;
import cube.common.entity.VoiceIndicator;
import cube.common.state.AIGCStateCode;

/**
 * 语音分割与分析监听器。
 */
public interface VoiceDiarizationListener {

    void onCompleted(FileLabel source, VoiceDiarization diarization, VoiceIndicator indicator);

    void onFailed(FileLabel source, AIGCStateCode stateCode);
}
