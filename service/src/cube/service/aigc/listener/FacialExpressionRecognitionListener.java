/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.listener;

import cube.common.entity.FacialExpression;
import cube.common.entity.FileLabel;
import cube.common.entity.SpeechEmotion;
import cube.common.state.AIGCStateCode;

import java.util.List;

/**
 * 面部表情识别监听器。
 */
public interface FacialExpressionRecognitionListener {

    void onCompleted(FileLabel input, List<FacialExpression> facialExpressions);

    void onFailed(FileLabel input, AIGCStateCode stateCode);
}
