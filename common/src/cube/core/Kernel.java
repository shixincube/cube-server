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
import cube.cache.TimeseriesCache;
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
     * @param name
     * @param module
     */
    public void installModule(String name, AbstractModule module) {
        this.moduleMap.put(name, module);
    }

    /**
     * 卸载模块。
     * @param name
     */
    public void uninstallModule(String name) {
        AbstractModule module = this.moduleMap.remove(name);
        if (null != module) {
            module.stop();
        }
    }

    /**
     * 获取指定名称的模块。
     * @param name
     * @return
     */
    public AbstractModule getModule(String name) {
        return this.moduleMap.get(name);
    }

    /**
     * 安装缓存。
     * @param name
     * @param config
     */
    public void installCache(String name, JSONObject config) {
        AbstractCache cache = null;
        try {
            String type = config.getString("type");
            if (SharedMemoryCache.TYPE.equalsIgnoreCase(type)) {
                cache = new SharedMemoryCache(name);
                cache.configure(config);
            }
            else if (TimeseriesCache.TYPE.equalsIgnoreCase(type)) {
                cache = new TimeseriesCache(name);
                cache.configure(config);
            }
        } catch (Exception e) {
            Logger.w(this.getClass(), "installCache", e);
        }

        if (null == cache) {
            return;
        }

        this.cacheMap.put(name, cache);
    }

    /**
     * 卸载缓存。
     * @param name
     */
    public void uninstallCache(String name) {
        AbstractCache cache = this.cacheMap.remove(name);
        if (null != cache) {
            cache.stop();
        }
    }

    /**
     * 获取指定名称缓存器。
     * @param name
     * @return
     */
    public Cache getCache(String name) {
        return this.cacheMap.get(name);
    }

    /**
     *
     * @param name
     * @param config
     */
    public void installMQ(String name, JSONObject config) {
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
            return;
        }

        this.mqMap.put(name, mq);
    }

    /**
     *
     * @param name
     */
    public void uninstallMQ(String name) {
        AbstractMQ mq = this.mqMap.get(name);
        if (null != mq) {
            mq.stop();
        }
    }

    /**
     *
     * @param name
     * @return
     */
    public AbstractMQ getMQ(String name) {
        return this.mqMap.get(name);
    }
}
