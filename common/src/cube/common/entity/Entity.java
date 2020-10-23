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

package cube.common.entity;

import cube.common.Domain;
import cube.common.JSONable;

/**
 * 实体对象基类。
 */
public abstract class Entity implements JSONable {

    /**
     * 实体创建的时间戳。
     */
    private long timestamp;

    /**
     * 实体所在域。
     */
    protected Domain domain;

    public Entity() {
        this.timestamp = System.currentTimeMillis();
    }

    public Entity(String domainName) {
        this.timestamp = System.currentTimeMillis();
        this.domain = new Domain(domainName);
    }

    public Entity(Domain domain) {
        this.timestamp = System.currentTimeMillis();
        this.domain = domain;
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
     * 获取实体所在域。
     *
     * @return 返回实体所在域。
     */
    public Domain getDomain() {
        return this.domain;
    }
}
