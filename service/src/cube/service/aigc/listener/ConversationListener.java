/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.listener;

import cube.common.entity.AIGCChannel;
import cube.common.entity.AIGCConversationResponse;
import cube.common.state.AIGCStateCode;

/**
 * Conversation 监听器。
 */
public interface ConversationListener {

    void onConversation(AIGCChannel channel, AIGCConversationResponse response);

    void onFailed(AIGCChannel channel, AIGCStateCode errorCode);
}
