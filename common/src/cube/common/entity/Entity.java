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

package cube.common.entity;

import cube.common.Domain;
import cube.common.JSONable;
import cube.common.UniqueKey;
import org.json.JSONObject;

/**
 * 实体对象基类。
 */
public abstract class Entity implements JSONable {

    /**
     * 实体 ID 。
     */
    protected Long id;

    /**
     * 实体所在域。
     */
    protected Domain domain;

    /**
     * 唯一索引键。
     */
    protected String uniqueKey;

    /**
     * 时间戳。
     */
    protected long timestamp;

    /**
     * 构造函数。
     */
    public Entity() {
        this.id = 0L;
        this.domain = new Domain("");
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 构造函数。
     *
     * @param json 实体的 JSON 数据结构。
     */
    public Entity(JSONObject json) {
        if (json.has("id")) {
            this.id = json.getLong("id");
        }
        if (json.has("domain")) {
            this.domain = new Domain(json.getString("domain"));
        }
        if (json.has("timestamp")) {
            this.timestamp = json.getLong("timestamp");
        }
        else {
            this.timestamp = System.currentTimeMillis();
        }

        if (null != this.id && null != this.domain) {
            this.uniqueKey = UniqueKey.make(this.id, this.domain);
        }
    }

    /**
     * 构造函数。
     *
     * @param id 指定实体的 ID 。
     */
    public Entity(Long id) {
        this.id = id;
        this.domain = new Domain("");
        this.uniqueKey = UniqueKey.make(id, "");
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 构造函数。
     *
     * @param id 指定实体 ID 。
     * @param domainName 指定所在域。
     */
    public Entity(Long id, String domainName) {
        this.id = id;
        this.domain = new Domain(domainName);
        this.uniqueKey = UniqueKey.make(id, domainName);
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 构造函数。
     *
     * @param id 指定实体 ID 。
     * @param domainName 指定所在域。
     * @param timestamp 时间戳。
     */
    public Entity(Long id, String domainName, long timestamp) {
        this.id = id;
        this.domain = new Domain(domainName);
        this.uniqueKey = UniqueKey.make(id, domainName);
        this.timestamp = timestamp;
    }

    /**
     * 构造函数。
     *
     * @param id 指定实体 ID 。
     * @param domain 指定所在域。
     */
    public Entity(Long id, Domain domain) {
        this.id = id;
        this.domain = domain;
        this.uniqueKey = UniqueKey.make(id, domain.getName());
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 获取实体 ID 。
     *
     * @return 返回实体 ID 。
     */
    public Long getId() {
        return this.id;
    }

    /**
     * 获取实体所在域。
     *
     * @return 返回实体所在域。
     */
    public Domain getDomain() {
        return this.domain;
    }

    /**
     * 获取唯一索引键。
     *
     * @return 返回唯一索引键。
     */
    public String getUniqueKey() {
        return this.uniqueKey;
    }

    /**
     * 获取实体创建的时间戳。
     *
     * @return 返回实体创建的时间戳。
     */
    public long getTimestamp() {
        return this.timestamp;
    }

    /**
     * 重置时间戳。
     */
    public void resetTimestamp() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 设置时间戳。
     *
     * @param timestamp
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * 设置域。
     *
     * @param domainName
     */
    public void setDomain(String domainName) {
        this.domain = new Domain(domainName);
    }

    @Override
    public boolean equals(Object object) {
        if (null != object && object instanceof Entity) {
            Entity other = (Entity) object;
            if (null != this.domain) {
                if (this.id.longValue() == other.id.longValue() && this.domain.equals(other.domain)) {
                    return true;
                }
            }
            else {
                if (this.id.longValue() == other.id.longValue()) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.uniqueKey.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id.longValue());
        json.put("domain", this.domain.getName());
        json.put("timestamp", this.timestamp);
        return json;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id.longValue());
        json.put("domain", this.domain.getName());
        json.put("timestamp", this.timestamp);
        return json;
    }

    /**
     * 更新时间戳。
     *
     * @param data
     * @param timestamp
     */
    public static void updateTimestamp(JSONObject data, long timestamp) {
        data.put("timestamp", timestamp);
    }
}
