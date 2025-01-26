/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.listener;

import cube.common.entity.AIGCChannel;
import cube.common.entity.KnowledgeQAResult;
import cube.common.state.AIGCStateCode;

/**
 * 知识库文档监听器。
 */
public interface KnowledgeQAListener {

    void onCompleted(AIGCChannel channel, KnowledgeQAResult result);

    void onFailed(AIGCChannel channel, AIGCStateCode stateCode);
}
