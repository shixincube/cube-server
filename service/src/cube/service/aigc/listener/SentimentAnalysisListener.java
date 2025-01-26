/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.listener;

import cube.common.entity.SentimentResult;

/**
 * 情感分析监听器。
 * @deprecated 2024-12-13
 */
public interface SentimentAnalysisListener {

    void onCompleted(SentimentResult result);

    void onFailed();
}
