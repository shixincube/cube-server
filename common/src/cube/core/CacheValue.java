/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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
 * 缓存器主键对应的数据值。
 */
public class CacheValue {

    /**
     * 数据值。
     */
    protected JSONObject value;

    /**
     * 对象类型的值。
     */
    protected Object objectValue = null;

    /**
     * 数据对应的时间戳。
     */
    protected long timestamp = 0;

    /**
     * 构造函数。
     *
     * @param value 指定数据值。
     */
    public CacheValue(JSONObject value) {
        this.value = value;
    }

    /**
     * 构造函数。
     *
     * @param value 指定对象实例类型的值。
     */
    public CacheValue(Object value) {
        this.objectValue = value;
    }

    /**
     * 构造函数。
     *
     * @param value 指定数据值。
     * @param timestamp 指定时间戳。
     */
    public CacheValue(JSONObject value, long timestamp) {
        this.value = value;
        this.timestamp = timestamp;
    }

    /**
     * 获取 JSON 形式的数据值。
     *
     * @return 返回 JSON 形式的数据值。
     */
    public JSONObject get() {
        return this.value;
    }

    /**
     * 设置对象实例值。
     *
     * @param value 对象实例。
     */
    public void setObjectValue(Object value) {
        this.objectValue = value;
    }

    /**
     * 返回对象实例值。
     *
     * @return 返回对象实例值。
     */
    public Object getObjectValue() {
        return this.objectValue;
    }

    /**
     * 获取数据的时间戳。
     *
     * @return 返回数据的时间戳。
     */
    public long getTimestamp() {
        return this.timestamp;
    }

    @Override
    public boolean equals(Object object) {
        if (null != object && object instanceof CacheValue) {
            CacheValue other = (CacheValue) object;
            if (other.value.toString().equals(this.value.toString())) {
                return true;
            }
        }

        return false;
    }
}
