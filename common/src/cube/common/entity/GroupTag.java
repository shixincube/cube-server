/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

/**
 * 群组标签。
 * 群组标签的主要作用是标记群组的功能分类。
 */
public final class GroupTag {

    private GroupTag() {
    }

    /**
     * 该群组可被用于公开场合。
     */
    public final static String Public = "public";

    /**
     * 该群组仅被用于会话场合。
     */
    public final static String Conversation = "conversation";

    /**
     * 该群组仅被用于会议场合。
     */
    public final static String Conference = "conference";
}
