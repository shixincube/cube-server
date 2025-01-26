/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.storage;

/**
 * 存储类型。
 */
public enum StorageType {

    /**
     * 基于 SQLite 实现的存储。
     */
    SQLite,

    /**
     * 基于 MySQL 实现的存储。
     */
    MySQL,

    /**
     * 其他存储。
     */
    Other
}
