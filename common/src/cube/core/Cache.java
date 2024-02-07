/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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
 * 缓存器接口。
 */
public interface Cache {

    /**
     * 返回缓存器名称。
     *
     * @return 返回缓存器名称。
     */
    public String getName();

    /**
     * 返回缓存器类型。
     *
     * @return 返回缓存器类型。
     */
    public String getType();

    /**
     * 返回缓存的配置信息。
     *
     * @return 返回缓存的配置信息。
     */
    public JSONObject getConfig();

    /**
     * 配置缓存器。
     *
     * @param config 指定配置信息和参数。
     */
    public void configure(JSONObject config);

    /**
     * 启动缓存。
     */
    public void start();

    /**
     * 停止缓存。
     */
    public void stop();

    /**
     * 写入数据。
     *
     * @param key 数据主键。
     * @param value 数据值。
     */
    public void put(CacheKey key, CacheValue value);

    /**
     * 读取数据。
     *
     * @param key 数据主键。
     * @return 返回数据值。
     */
    public CacheValue get(CacheKey key);

    /**
     * 以表达式描述的方式读取数据。
     *
     * @param expression 缓存表达式。
     * @return 返回数据值。
     */
    public CacheValue get(CacheExpression expression);

    /**
     * 移除主键对应的数据。
     *
     * @param key 数据主键。
     */
    public void remove(CacheKey key);

    /**
     * 执行缓存事务。
     *
     * @param key 主键。
     * @param transaction 待执行的事务。
     */
    public void execute(CacheKey key, CacheTransaction transaction);

}
