/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
