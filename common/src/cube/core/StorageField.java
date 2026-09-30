/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.core;

import cell.core.talk.LiteralBase;

/**
 * 存储字段。
 *
 * <h2>不可变性</h2>
 * <p>本类实例在构造完成后<b>不再改变</b>：所有字段均为 {@code final}，
 * 构造器对传入的约束数组做防御性拷贝，{@link #getConstraints()} 返回内部数组的副本。
 * 调用方修改 {@code getConstraints()} 的返回值不会影响本实例。</p>
 *
 * <p>历史实现提供 {@code resetLiteralBase()} / {@code setValue()} / {@code setConstraints()}
 * 三个就地修改方法，存储实现（如 SQLite 需要把 {@code BIGINT} 改写为 {@code INTEGER}）
 * 会借助它们<b>直接改写调用方的对象</b>：同一份字段数组既用于建表又用于查询时，
 * 查询会读到被改写后的字面义，长整型存在被降级为 {@code int} 的风险。
 * 现在改为「构造修正后的新实例」，调用方持有的对象始终保持原样。</p>
 */
public class StorageField {

    /**
     * 字段名。
     */
    private final String name;

    /**
     * 字段数据类型字面义。
     */
    private final LiteralBase literalBase;

    /**
     * 字段数据值。
     */
    private final Object value;

    /**
     * 字段约束。
     */
    private final Constraint[] constraints;

    /**
     * JOIN 时的表名。
     */
    private final String tableName;

    /**
     * 构造函数。
     *
     * @param name 字段名。
     * @param literalBase 字段的数据类型字面义。
     */
    public StorageField(String name, LiteralBase literalBase) {
        this(null, name, literalBase, null, null);
    }

    /**
     * 构造函数。
     *
     * @param name 字段名。
     * @param value 字段值。
     */
    public StorageField(String name, long value) {
        this(null, name, LiteralBase.LONG, value, null);
    }

    /**
     * 构造函数。
     *
     * @param name 字段名。
     * @param value 字段值。
     */
    public StorageField(String name, Long value) {
        this(null, name, LiteralBase.LONG, value, null);
    }

    /**
     * 构造函数。
     *
     * @param name 字段名。
     * @param value 字段值。
     */
    public StorageField(String name, int value) {
        this(null, name, LiteralBase.INT, value, null);
    }

    /**
     * 构造函数。
     *
     * @param name 字段名。
     * @param value 字段值。
     */
    public StorageField(String name, Integer value) {
        this(null, name, LiteralBase.INT, value, null);
    }

    /**
     * 构造函数。
     *
     * @param name 字段名。
     * @param value 字段值。
     */
    public StorageField(String name, String value) {
        this(null, name, LiteralBase.STRING, value, null);
    }

    /**
     * 构造函数。
     *
     * @param name 字段名。
     * @param literalBase 字段的数据类型字面义。
     * @param value 字段值。
     */
    public StorageField(String name, LiteralBase literalBase, Object value) {
        this(null, name, literalBase, value, null);
    }

    /**
     * 构造函数。
     *
     * @param tableName 表名。
     * @param name 字段名。
     * @param literalBase 字段的数据类型字面义。
     */
    public StorageField(String tableName, String name, LiteralBase literalBase) {
        this(tableName, name, literalBase, null, null);
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
        this(tableName, name, literalBase, value, null);
    }

    /**
     * 构造函数。
     *
     * @param name 字段名。
     * @param literalBase 字段的数据类型字面义。
     * @param constraints 字段约束。
     */
    public StorageField(String name, LiteralBase literalBase, Constraint[] constraints) {
        this(null, name, literalBase, null, constraints);
    }

    /**
     * 构造函数。
     *
     * <p>约束数组会被拷贝，调用方之后修改传入的数组不会影响本实例。</p>
     *
     * @param tableName 表名。
     * @param name 字段名。
     * @param literalBase 字段的数据类型字面义。
     * @param value 数据值。
     * @param constraints 字段约束。
     */
    public StorageField(String tableName, String name, LiteralBase literalBase, Object value,
                        Constraint[] constraints) {
        this.tableName = tableName;
        this.name = name;
        this.literalBase = literalBase;
        this.value = value;
        this.constraints = (null == constraints) ? null : constraints.clone();
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
     * 获取表名。
     *
     * @return 返回表名。
     */
    public String getTableName() {
        return this.tableName;
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
     * 返回约束。
     *
     * <p>返回内部数组的副本，调用方对返回值的修改不会影响本实例。</p>
     *
     * @return 返回约束数组，未设置时返回 {@code null} 。
     */
    public Constraint[] getConstraints() {
        return (null == this.constraints) ? null : this.constraints.clone();
    }
}
