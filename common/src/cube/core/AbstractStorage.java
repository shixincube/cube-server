/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.core;

import cube.storage.StorageType;
import org.json.JSONObject;

/**
 * 存储器的抽象层。
 */
public abstract class AbstractStorage implements Storage {

    /**
     * 存储名称。
     */
    private String name;

    /**
     * 实现类型。
     */
    protected StorageType type;

    /**
     * 配置信息。
     */
    protected JSONObject config;

    /**
     * 构造函数。
     *
     * @param name 指定存储名称。
     */
    public AbstractStorage(String name, StorageType type) {
        this.name = name;
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     */
    public StorageType getType() {
        return this.type;
    }

    /**
     * {@inheritDoc}
     */
    public JSONObject getConfig() {
        return this.config;
    }

    /**
     * {@inheritDoc}
     */
    public void configure(JSONObject config) {
        this.config = config;
    }

}
