/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.listener;

import cube.common.state.AIGCStateCode;

/**
 * 生成摘要监听器。
 */
public interface SummarizationListener {

    void onCompleted(String text, String summarization);

    void onFailed(String text, AIGCStateCode stateCode);
}
