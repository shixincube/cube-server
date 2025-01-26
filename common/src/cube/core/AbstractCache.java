/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.core;

import org.json.JSONObject;

/**
 * 缓存抽象层。
 */
public abstract class AbstractCache implements Cache {

    /**
     * 缓存名称。
     */
    private String name;

    /**
     * 缓存类型描述。
     */
    private String type;

    /**
     * 缓存配置信息。
     */
    private JSONObject config;

    /**
     * 构造函数。
     *
     * @param name 缓存名称。
     */
    public AbstractCache(String name, String type) {
        this.name = name;
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return this.type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject getConfig() {
        return this.config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(JSONObject config) {
        this.config = config;
    }
}
