/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

/**
 * 知识库文档范围。
 */
public enum KnowledgeScope {

    /**
     * 全局。
     */
    Global("global"),

    /**
     * 公共。
     */
    Public("public"),

    /**
     * 私有。
     */
    Private("private")

    ;

    public final String name;

    KnowledgeScope(String name) {
        this.name = name;
    }

    public static KnowledgeScope parse(String name) {
        for (KnowledgeScope scope : KnowledgeScope.values()) {
            if (scope.name.equalsIgnoreCase(name)) {
                return scope;
            }
        }
        return KnowledgeScope.Private;
    }
}
