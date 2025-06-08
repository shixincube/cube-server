/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.listener;

import cube.common.entity.KnowledgeDocument;
import cube.common.entity.ResetKnowledgeProgress;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.knowledge.KnowledgeBase;

import java.util.List;

/**
 * 重置知识仓库监听器。
 */
public interface ResetKnowledgeStoreListener {

    void onProgress(KnowledgeBase knowledgeBase, ResetKnowledgeProgress progress);

    void onFailed(KnowledgeBase knowledgeBase, ResetKnowledgeProgress progress, AIGCStateCode stateCode);

    void onCompleted(KnowledgeBase knowledgeBase, List<KnowledgeDocument> originList, List<KnowledgeDocument> completionList);
}
