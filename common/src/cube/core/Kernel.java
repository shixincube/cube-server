/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

import cell.util.json.JSONObject;
import cell.util.log.Logger;
import cube.cache.SharedMemoryCache;
import cube.cache.SeriesMemoryTimeSeriesCache;
import cube.mq.AdapterMQ;

import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务器上的内核。
 */
public final class Kernel {

    /**
     * 服务器节点名。
     */
    private String nodeName;

    /**
     * 存储模块的映射。
     */
    private ConcurrentHashMap<String, AbstractModule> moduleMap;

    /**
     * 存储缓存的映射。
     */
    private ConcurrentHashMap<String, AbstractCache> cacheMap;

    /**
     * 存储消息队列的映射。
     */
    private ConcurrentHashMap<String, AbstractMQ> mqMap;

    /**
     * 是否已启动。
     */
    private boolean started = false;

    /**
     * 构造函数。
     */
    public Kernel() {
        this.moduleMap = new ConcurrentHashMap<>();
        this.cacheMap = new ConcurrentHashMap<>();
        this.mqMap = new ConcurrentHashMap<>();
        this.nodeName = UUID.randomUUID().toString();
    }

    /**
     * 设置节点名。
     *
     * @param name 指定节点名。
     */
    public void setNodeName(String name) {
        this.nodeName = name;
    }

    /**
     * 获取节点名。
     *
     * @return 返回节点名。
     */
    public String getNodeName() {
        return this.nodeName;
    }

    /**
     * 启动内核。
     */
    public void startup() {
        this.started = true;

        Iterator<AbstractCache> citer = this.cacheMap.values().iterator();
        while (citer.hasNext()) {
            AbstractCache cache = citer.next();
            cache.start();
        }

        Iterator<AbstractMQ> mqiter = this.mqMap.values().iterator();
        while (mqiter.hasNext()) {
            AbstractMQ mq = mqiter.next();
            mq.start();
        }

        Iterator<AbstractModule> miter = this.moduleMap.values().iterator();
        while (miter.hasNext()) {
            AbstractModule module = miter.next();
            module.setKernel(this);
        }

        miter = this.moduleMap.values().iterator();
        while (miter.hasNext()) {
            AbstractModule module = miter.next();
            module.start();
        }
    }

    /**
     * 停止内核。
     */
    public void shutdown() {
        Iterator<AbstractModule> miter = this.moduleMap.values().iterator();
        while (miter.hasNext()) {
            AbstractModule module = miter.next();
            module.stop();
        }

        Iterator<AbstractMQ> mqiter = this.mqMap.values().iterator();
        while (mqiter.hasNext()) {
            AbstractMQ mq = mqiter.next();
            mq.stop();
        }

        Iterator<AbstractCache> citer = this.cacheMap.values().iterator();
        while (citer.hasNext()) {
            AbstractCache cache = citer.next();
            cache.stop();
        }
    }

    /**
     * 安装模块。
     *
     * @param name 模块名称。
     * @param module 模块实例。
     */
    public void installModule(String name, AbstractModule module) {
        this.moduleMap.put(name, module);
    }

    /**
     * 卸载模块。
     *
     * @param name 模块名称。
     */
    public void uninstallModule(String name) {
        AbstractModule module = this.moduleMap.remove(name);
        if (null != module) {
            module.stop();
        }
    }

    /**
     * 获取指定名称的模块。
     *
     * @param name 指定模块名。
     * @return 返回对应模块的实例。
     */
    public AbstractModule getModule(String name) {
        return this.moduleMap.get(name);
    }

    /**
     * 安装缓存。
     *
     * @param name 指定缓存名称。
     * @param config 指定缓存配置信息。
     */
    public AbstractCache installCache(String name, JSONObject config) {
        AbstractCache cache = null;
        try {
            String type = config.getString("type");
            if (SharedMemoryCache.TYPE.equalsIgnoreCase(type)) {
                cache = new SharedMemoryCache(name);
                cache.configure(config);
            }
        } catch (Exception e) {
            Logger.w(this.getClass(), "installCache", e);
        }

        if (null == cache) {
            return null;
        }

        this.cacheMap.put(name, cache);

        if (this.started) {
            cache.start();
        }

        return cache;
    }

    /**
     * 卸载缓存。
     *
     * @param name 指定缓存名称。
     */
    public void uninstallCache(String name) {
        AbstractCache cache = this.cacheMap.remove(name);
        if (null != cache) {
            cache.stop();
        }
    }

    /**
     * 获取指定名称缓存器。
     *
     * @param name 指定缓存器名称。
     * @return 返回缓存器实例。
     */
    public Cache getCache(String name) {
        return this.cacheMap.get(name);
    }

    /**
     * 获取指定名称时序缓存器。
     *
     * @param name 指定缓存器名称。
     * @return 返回时序缓存器实例。
     */
    public TimeSeriesCache getTimeSeriesCache(String name) {
        return null;
    }

    /**
     * 安装消息队列。
     *
     * @param name 指定消息队列名称。
     * @param config 指定队列配置信息。
     */
    public AbstractMQ installMQ(String name, JSONObject config) {
        AbstractMQ mq = null;
        try {
            String type = config.getString("type");
            if (AdapterMQ.TYPE.equalsIgnoreCase(type)) {
                mq = new AdapterMQ(name);
            }
        } catch (Exception e) {
            Logger.w(this.getClass(), "installMQ", e);
        }

        if (null == mq) {
            return null;
        }

        this.mqMap.put(name, mq);

        if (this.started) {
            mq.start();
        }

        return mq;
    }

    /**
     * 卸载消息队列。
     *
     * @param name 指定队列名称。
     */
    public void uninstallMQ(String name) {
        AbstractMQ mq = this.mqMap.get(name);
        if (null != mq) {
            mq.stop();
        }
    }

    /**
     * 获取指定名称的消息队列。
     *
     * @param name 指定队列名称。
     * @return 返回消息队列实例。
     */
    public AbstractMQ getMQ(String name) {
        return this.mqMap.get(name);
    }
}
