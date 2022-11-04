/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
