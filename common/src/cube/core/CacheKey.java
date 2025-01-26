/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.core;

/**
 * 缓存器的主键描述。
 */
public class CacheKey {

    /**
     * 主键。
     */
    protected String key;

    /**
     * 构造函数。
     *
     * @param key 指定键。
     */
    public CacheKey(String key) {
        this.key = key;
    }

    /**
     * 构造函数。
     *
     * @param key 指定键。
     */
    public CacheKey(Long key) {
        this.key = key.toString();
    }

    /**
     * 获取主键。
     *
     * @return 返回主键。
     */
    public String get() {
        return this.key;
    }

    @Override
    public boolean equals(Object object) {
        if (null != object && object instanceof CacheKey) {
            CacheKey other = (CacheKey) object;
            if (other.key.equals(this.key)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }
}
