/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.listener;

import cube.common.entity.AIGCChannel;
import cube.common.entity.MultimodalOutput;
import cube.common.state.AIGCStateCode;

/**
 * 多模态监听器。
 */
public interface MultimodalListener {

    void onResponse(AIGCChannel channel, MultimodalOutput response);

    void onFailed(AIGCChannel channel, AIGCStateCode errorCode);
}
