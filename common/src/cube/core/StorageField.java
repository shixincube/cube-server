/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.core;

import cell.core.talk.LiteralBase;

/**
 * 存储字段。
 */
public class StorageField {

    /**
     * 字段名。
     */
    private String name;

    /**
     * 字段数据类型字面义。
     */
    private LiteralBase literalBase;

    /**
     * 字段数据值。
     */
    private Object value;

    /**
     * 字段约束。
     */
    private Constraint[] constraints;

    /**
     * JOIN 时的表名。
     */
    private String tableName;

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
     * @param value 字段值。
     */
    public StorageField(String name, long value) {
        this.name = name;
        this.literalBase = LiteralBase.LONG;
        this.value = value;
    }

    /**
     * 构造函数。
     *
     * @param name 字段名。
     * @param value 字段值。
     */
    public StorageField(String name, Long value) {
        this.name = name;
        this.literalBase = LiteralBase.LONG;
        this.value = value;
    }

    /**
     * 构造函数。
     *
     * @param name 字段名。
     * @param value 字段值。
     */
    public StorageField(String name, int value) {
        this.name = name;
        this.literalBase = LiteralBase.INT;
        this.value = value;
    }

    /**
     * 构造函数。
     *
     * @param name 字段名。
     * @param value 字段值。
     */
    public StorageField(String name, Integer value) {
        this.name = name;
        this.literalBase = LiteralBase.INT;
        this.value = value;
    }

    /**
     * 构造函数。
     *
     * @param name 字段名。
     * @param value 字段值。
     */
    public StorageField(String name, String value) {
        this.name = name;
        this.literalBase = LiteralBase.STRING;
        this.value = value;
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
     * 构造函数。
     *
     * @param tableName 表名。
     * @param name 字段名。
     * @param literalBase 字段的数据类型字面义。
     */
    public StorageField(String tableName, String name, LiteralBase literalBase) {
        this.tableName = tableName;
        this.name = name;
        this.literalBase = literalBase;
    }

    /**
     * 构造函数。
     *
     * @param tableName 表名。
     * @param name 字段名。
     * @param literalBase 字段的数据类型字面义。
     * @param value 数据值。
     */
    public StorageField(String tableName, String name, LiteralBase literalBase, Object value) {
        this.tableName = tableName;
        this.name = name;
        this.literalBase = literalBase;
        this.value = value;
    }

    /**
     * 构造函数。
     *
     * @param name 字段名。
     * @param literalBase 字段的数据类型字面义。
     * @param constraints 字段约束。
     */
    public StorageField(String name, LiteralBase literalBase, Constraint[] constraints) {
        this.name = name;
        this.literalBase = literalBase;
        this.constraints = constraints;
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
     * 重置字面义。
     *
     * @param base 新的字面义。
     */
    public void resetLiteralBase(LiteralBase base) {
        this.literalBase = base;
    }

    /**
     * 获取表名。
     *
     * @return 返回表名。
     */
    public String getTableName() {
        return this.tableName;
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
     * 是否为空值。
     *
     * @return 如果是空值返回 {@code true} 。
     */
    public boolean isNullValue() {
        return (null == this.value);
    }

    /**
     * 返回 int 类型值。
     *
     * @return 返回 int 类型值。
     */
    public int getInt() {
        if (this.value instanceof Integer) {
            return (Integer) this.value;
        }
        else if (this.value instanceof Long) {
            return ((Long)this.value).intValue();
        }
        else if (this.value instanceof String) {
            return Integer.parseInt((String) this.value);
        }
        else {
            return Integer.parseInt(this.value.toString());
        }
    }

    /**
     * 返回 long 类型值。
     *
     * @return 返回 int 类型值。
     */
    public long getLong() {
        if (null == this.value) {
            return 0;
        }

        if (this.value instanceof Long) {
            return (Long) this.value;
        }
        else if (this.value instanceof Integer) {
            return (Integer) this.value;
        }
        else if (this.value instanceof String) {
            return Long.parseLong((String) this.value);
        }
        else {
            return Long.parseLong(this.value.toString());
        }
    }

    /**
     * 返回 String 类型值。
     *
     * @return 返回 String 类型值。
     */
    public String getString() {
        if (null == this.value) {
            return null;
        }

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

    /**
     * 设置约束。
     *
     * @param constraints
     */
    public void setConstraints(Constraint[] constraints) {
        this.constraints = constraints;
    }

    /**
     * 返回约束。
     *
     * @return
     */
    public Constraint[] getConstraints() {
        return this.constraints;
    }
}
