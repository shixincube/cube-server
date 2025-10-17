/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.listener;

import cube.common.entity.FacialExpressionResult;
import cube.common.entity.FileLabel;
import cube.common.state.AIGCStateCode;

/**
 * 面部表情识别监听器。
 */
public interface FacialExpressionRecognitionListener {

    void onCompleted(FileLabel input, FacialExpressionResult result);

    void onFailed(FileLabel input, AIGCStateCode stateCode);
}
