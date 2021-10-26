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

import org.json.JSONObject;

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
    protected volatile boolean started;

    /**
     * 构造函数。
     */
    public AbstractModule() {
        this.started = false;
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
    public boolean isStarted() {
        return this.started;
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
    public JSONObject notify(JSONObject data) {
        return data;
    }
}
