/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.listener;

import cube.common.entity.AIGCChannel;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;

/**
 * 文本生成文件监听器。
 */
public interface TextToFileListener {

    void onProcessing(AIGCChannel channel);

    void onCompleted(GeneratingRecord result);

    void onFailed(AIGCChannel channel, AIGCStateCode stateCode);
}
