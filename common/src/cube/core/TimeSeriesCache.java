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

import java.util.List;

/**
 * 时序缓存器。
 */
public interface TimeSeriesCache {

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
     * 向缓存里按照当前时间戳追加数据。
     *
     * @param key 数据键。
     * @param value 数据值。
     */
    public void add(CacheKey key, CacheValue value);

    /**
     * 向缓存里按照指定时间戳追加数据。
     *
     * @param key 数据键。
     * @param value 数据值。
     * @param timestamp 指定时间戳。
     */
    public void add(CacheKey key, CacheValue value, long timestamp);

    /**
     * 查询指定时间范围内数据键对应的数据。
     *
     * @param key 数据键。
     * @param beginningTime 查询的起始时间戳。
     * @param endingTime 查询的截止时间戳。
     * @return 返回数据列表。
     */
    public List<CacheValue> query(CacheKey key, long beginningTime, long endingTime);

    /**
     * 删除指定时间戳之前的数据。
     *
     * @param key 数据键。
     * @param timestamp 指定时间戳。
     */
    public void delete(CacheKey key, long timestamp);
}
