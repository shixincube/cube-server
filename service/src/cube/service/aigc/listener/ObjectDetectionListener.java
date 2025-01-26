/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.listener;

import cube.common.entity.FileLabel;
import cube.common.entity.ObjectDetectionResult;
import cube.common.state.AIGCStateCode;

import java.util.List;

/**
 * 物体检测监听器。
 */
public interface ObjectDetectionListener {

    void onCompleted(List<FileLabel> inputList, List<ObjectDetectionResult> resultList);

    void onFailed(List<FileLabel> sourceList, AIGCStateCode stateCode);
}
