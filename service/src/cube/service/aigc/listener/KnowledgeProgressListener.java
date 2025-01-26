/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.listener;

import cube.common.entity.KnowledgeProgress;
import cube.service.aigc.knowledge.KnowledgeBase;

/**
 * 批量操作知识库监听器。
 */
public interface KnowledgeProgressListener {

    void onProgress(KnowledgeBase knowledgeBase, KnowledgeProgress progress);

    void onFailed(KnowledgeBase knowledgeBase, KnowledgeProgress progress);

    void onCompleted(KnowledgeBase knowledgeBase, KnowledgeProgress progress);
}
