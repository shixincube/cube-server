/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.core;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 模块抽象层。
 */
public abstract class AbstractModule implements Module {

    /**
     * 内核实例。
     */
    private Kernel kernel;

    /**
     * 是否已启动。
     */
    protected final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * 构造函数。
     */
    public AbstractModule() {
    }

    /**
     * 设置内核实例。
     *
     * @param kernel 内核实例。
     */
    protected void setKernel(Kernel kernel) {
        this.kernel = kernel;
    }

    /**
     * 获取内核实例。
     *
     * @return 返回内核实例。
     */
    public Kernel getKernel() {
        return this.kernel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        // Nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStarted() {
        return this.started.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cache getCache(String name) {
        return this.kernel.getCache(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageQueue getMQ(String name) {
        return this.kernel.getMQ(name);
    }

    /**
     * 向模块送达数据并返回结果。
     *
     * @param data
     * @return
     */
    public <T> T notify(Object data) {
        return (T) data;
    }
}
