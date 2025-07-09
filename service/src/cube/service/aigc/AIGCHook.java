/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc;

import cube.plugin.Hook;

/**
 * AIGC 钩子。
 */
public class AIGCHook extends Hook {

    /**
     * 导入知识文档。
     */
    public final static String ImportKnowledgeDoc = "ImportKnowledgeDoc";

    /**
     * 移除知识文档。
     */
    public final static String RemoveKnowledgeDoc = "RemoveKnowledgeDoc";

    /**
     * 应用层事件。
     */
    public final static String AppEvent = "AppEvent";

    public AIGCHook(String key) {
        super(key);
    }
}
