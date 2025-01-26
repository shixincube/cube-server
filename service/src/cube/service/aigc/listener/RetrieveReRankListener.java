/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.listener;

import cube.common.entity.RetrieveReRankResult;
import cube.common.state.AIGCStateCode;

import java.util.List;

/**
 * 语义搜索监听器。
 */
public interface RetrieveReRankListener {

    void onCompleted(List<RetrieveReRankResult> retrieveReRankResults);

    void onFailed(List<String> queries, AIGCStateCode stateCode);
}
