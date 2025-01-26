/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.core;

import org.json.JSONObject;

/**
 * 消息队列的抽象层。
 */
public abstract class AbstractMQ implements MessageQueue {

    /**
     * 队列名称。
     */
    private String name;

    /**
     * 队列配置。
     */
    private JSONObject config;

    /**
     * 构造函数。
     *
     * @param name 指定队列名称。
     */
    public AbstractMQ(String name) {
        this.name = name;
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
