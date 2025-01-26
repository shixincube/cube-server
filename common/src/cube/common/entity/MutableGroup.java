/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

/**
 * 用于异步操作时进行上下文操作的群组。
 */
public class MutableGroup {

    private Group group = null;

    public MutableGroup() {
    }

    public void set(Group group) {
        this.group = group;
    }

    public Group get() {
        return this.group;
    }

    public boolean isNull() {
        return (null == this.group);
    }
}
