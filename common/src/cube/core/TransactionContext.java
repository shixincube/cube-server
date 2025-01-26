/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.core;

/**
 * 缓存事务上下文。
 */
public abstract class TransactionContext {

    private CacheKey key;

    public TransactionContext(CacheKey key) {
        this.key = key;
    }

    public CacheKey getKey() {
        return this.key;
    }

    public abstract CacheValue get();

    public abstract void put(CacheValue value);

    public abstract void remove();
}
