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
 * 生成文本监听器。
 */
public interface GenerateTextListener {

    void onGenerated(AIGCChannel channel, GeneratingRecord record);

    void onFailed(AIGCChannel channel, AIGCStateCode stateCode);
}
