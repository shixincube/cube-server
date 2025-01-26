/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.core;

import cube.plugin.PluginSystem;

/**
 * 模块描述类。
 */
public interface Module {

    /**
     * 启动模块。
     */
    public void start();

    /**
     * 停止模块。
     */
    public void stop();

    /**
     * 是否已启动。
     *
     * @return 返回是否已启动模块。
     */
    public boolean isStarted();

    /**
     * 获取模块的插件系统。
     *
     * @return 返回模块的插件系统实例。
     */
    public <T extends PluginSystem> T getPluginSystem();

    /**
     * 获取指定名称的缓存器。
     *
     * @param name 缓存器名称。
     * @return 返回缓存实例。
     */
    public Cache getCache(String name);

    /**
     * 获取指定名称的消息队列。
     *
     * @param name 消息队列名称。
     * @return 返回消息队列实例。
     */
    public MessageQueue getMQ(String name);

    /**
     * 内核每一个维护周期回调该方法。
     *
     * @param kernel 当前内核实例。
     */
    public void onTick(Module module, Kernel kernel);
}
