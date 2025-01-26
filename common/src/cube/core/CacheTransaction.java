/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.core;

/**
 * 缓存事务。
 */
public interface CacheTransaction {

    /**
     * 执行事务。
     *
     * @param context 上下文 。
     */
    public void perform(TransactionContext context);

}
