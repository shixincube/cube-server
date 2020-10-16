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

import cell.core.talk.LiteralBase;

/**
 * 存储字段。
 */
public class StorageField {

    private String name;

    private LiteralBase literalBase;

    private Object value;

    /**
     * 构造函数。
     *
     * @param name 字段名。
     * @param literalBase 字段的数据类型字面义。
     */
    public StorageField(String name, LiteralBase literalBase) {
        this.name = name;
        this.literalBase = literalBase;
    }

    /**
     * 构造函数。
     *
     * @param name 字段名。
     * @param literalBase 字段的数据类型字面义。
     * @param value 字段值。
     */
    public StorageField(String name, LiteralBase literalBase, Object value) {
        this.name = name;
        this.literalBase = literalBase;
        this.value = value;
    }

    /**
     * 获取字段名。
     *
     * @return 返回字段名。
     */
    public String getName() {
        return this.name;
    }

    /**
     * 获取数据类型字面义。
     *
     * @return 返回数据类型字面义。
     */
    public LiteralBase getLiteralBase() {
        return this.literalBase;
    }

    /**
     * 设置字段值。
     *
     * @param value 值对象。
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * 获取字段值。
     *
     * @return 返回字段值。
     */
    public Object getValue() {
        return this.value;
    }

    /**
     * 返回 int 类型值。
     *
     * @return 返回 int 类型值。
     */
    public int getInt() {
        if (this.value instanceof Integer) {
            return ((Integer)this.value).intValue();
        }
        else if (this.value instanceof Long) {
            return ((Long)this.value).intValue();
        }
        else {
            return 0;
        }
    }

    /**
     * 返回 long 类型值。
     *
     * @return 返回 int 类型值。
     */
    public long getLong() {
        if (this.value instanceof Long) {
            return ((Long)this.value).longValue();
        }
        return 0;
    }

    /**
     * 返回 String 类型值。
     *
     * @return 返回 String 类型值。
     */
    public String getString() {
        if (this.value instanceof String) {
            return ((String)this.value);
        }
        else {
            return this.value.toString();
        }
    }

    /**
     * 返回 boolean 类型值。
     *
     * @return 返回 boolean 类型值。
     */
    public boolean getBoolean() {
        if (this.value instanceof Boolean) {
            return ((Boolean)this.value).booleanValue();
        }
        return false;
    }
}
