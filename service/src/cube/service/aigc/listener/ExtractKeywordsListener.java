/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.listener;

import cube.common.state.AIGCStateCode;

import java.util.List;

/**
 * 提取关键词监听器。
 */
public interface ExtractKeywordsListener {

    void onCompleted(String text, List<String> words);

    void onFailed(String text, AIGCStateCode stateCode);
}
