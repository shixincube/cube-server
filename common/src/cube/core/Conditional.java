/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.core;

import cell.core.talk.LiteralBase;
import cube.util.SQLUtils;

/**
 * 条件句式。
 *
 * <p><b>安全约定</b>：所有取值型句式都必须通过 {@link #renderValue(StorageField)}
 * 输出 SQL 字面量，禁止直接调用 {@code field.getValue().toString()} ——
 * 后者不会转义单引号与反斜杠，会形成注入点。</p>
 */
public class Conditional {

    public final static String Quote = "`";

    /**
     * LIKE 模式串使用的转义字符。
     *
     * <p>与 {@link cube.util.SQLUtils#correctString(String)} 的反斜杠转义保持一致，
     * 因此要求数据库运行在默认的 backslash-escapes 模式下。</p>
     */
    private final static char LIKE_ESCAPE_CHAR = '\\';

    /**
     * 句式的 SQL 语句。
     */
    private String sql;

    /**
     * 是否包含 WHERE 句式。
     */
    protected boolean whereSentence = false;

    /**
     * 构造函数。
     *
     * @param sql 指定 SQL 字符串。
     */
    protected Conditional(String sql) {
        this.sql = sql;
    }

    /**
     * 构造函数。
     *
     * @param sql 指定 SQL 字符串。
     * @param whereSentence 是否需要 WHERE 句式。
     */
    protected Conditional(String sql, boolean whereSentence) {
        this.sql = sql;
        this.whereSentence = whereSentence;
    }

    @Override
    public String toString() {
        return this.sql;
    }

    public boolean needWhereSentence() {
        return this.whereSentence;
    }

    /**
     * 将字段值渲染为可直接拼进 SQL 的字面量。
     *
     * <p>字符串类型会转义后加单引号；数值与布尔类型按原样输出；
     * 历史实现未覆盖的类型（FLOAT / DOUBLE / JSON 等）退化为转义字符串，
     * 避免"什么都不输出"造成 SQL 语法错误。</p>
     *
     * @param field 字段描述。
     * @return 返回 SQL 字面量。
     * @throws IllegalArgumentException 字段值为 {@code null} 时抛出。
     */
    private static String renderValue(StorageField field) {
        if (null == field.getValue()) {
            // 历史实现此处会抛 NullPointerException，这里改为显式失败并给出字段名
            throw new IllegalArgumentException("#renderValue - The value of field is null: " + field.getName());
        }

        LiteralBase literal = field.getLiteralBase();

        if (null == literal) {
            return "'" + SQLUtils.correctString(field.getString()) + "'";
        }

        switch (literal) {
            case INT:
                return String.valueOf(field.getInt());
            case LONG:
                return String.valueOf(field.getLong());
            case BOOL:
                return field.getBoolean() ? "1" : "0";
            case STRING:
            default:
                return "'" + SQLUtils.correctString(field.getString()) + "'";
        }
    }

    /**
     * 转义 LIKE 模式串。
     *
     * <p>通配符 {@code %} 与 {@code _} 以及转义字符 {@code \} 都必须被转义，
     * 否则用户输入的关键字会被当成通配符使用，导致匹配范围被放大。</p>
     *
     * @param pattern 原始模式串。
     * @return 返回转义后的模式串。
     */
    private static String escapeLikePattern(String pattern) {
        if (null == pattern) {
            return "";
        }

        // 顺序不可颠倒：先转义转义字符自身，再转义通配符，最后转义单引号
        String result = pattern.replace("\\", "\\\\");
        result = result.replace("%", "\\%");
        result = result.replace("_", "\\_");
        result = result.replace("'", "''");
        return result;
    }

    /**
     * 创建 AND 连接。
     *
     * @return 返回条件句式实例。
     */
    public static Conditional createAnd() {
        return new Conditional("AND");
    }

    /**
     * 创建 OR 连接。
     *
     * @return 返回条件句式实例。
     */
    public static Conditional createOr() {
        return new Conditional("OR");
    }

    /**
     * 创建括号操作。
     *
     * @param conditionals 括号内的表达式。
     * @return 返回括号句式实例。
     */
    public static Conditional createBracket(Conditional[] conditionals) {
        if (null == conditionals || conditionals.length == 0) {
            throw new IllegalArgumentException("#createBracket - Empty conditionals, refuse to build SQL");
        }

        StringBuilder buf = new StringBuilder("( ");
        for (Conditional cond : conditionals) {
            if (null == cond) {
                continue;
            }

            buf.append(cond.sql).append(" ");
        }
        buf.append(")");
        return new Conditional(buf.toString(), true);
    }

    /**
     * 创建字段 NULL 值判断。
     *
     * @param fieldName 指定字段名。
     * @return 返回句式实例。
     */
    public static Conditional createIsNull(String fieldName) {
        return new Conditional(Quote + fieldName + Quote + " IS NULL");
    }

    /**
     * 创建 LIMIT 约束。
     *
     * @param num 指定约束数量。
     * @return 返回条件句式实例。
     */
    public static Conditional createLimit(int num) {
        return new Conditional("LIMIT " + num);
    }

    /**
     * 创建 LIMIT 约束。
     *
     * @param pos 指定开始位置。
     * @param count 指定数量。
     * @return 返回条件句式实例。
     */
    public static Conditional createLimit(int pos, int count) {
        return new Conditional("LIMIT " + pos + "," + count);
    }

    /**
     * 创建等于运算。
     *
     * @param fieldName 字段名。
     * @param value 字段值。
     * @return 返回条件句式实例。
     */
    public static Conditional createEqualTo(String fieldName, int value) {
        return Conditional.createEqualTo(new StorageField(fieldName, value));
    }

    /**
     * 创建等于运算。
     *
     * @param fieldName 字段名。
     * @param value 字段值。
     * @return 返回条件句式实例。
     */
    public static Conditional createEqualTo(String fieldName, long value) {
        return Conditional.createEqualTo(new StorageField(fieldName, value));
    }

    /**
     * 创建等于运算。
     *
     * @param fieldName 字段名。
     * @param value 字段值。
     * @return 返回条件句式实例。
     */
    public static Conditional createEqualTo(String fieldName, String value) {
        return Conditional.createEqualTo(new StorageField(fieldName, value));
    }

    /**
     *  创建等于运算。
     *
     * @param fieldName 字段名。
     * @param literalBase 字段类型。
     * @param value 字段值。
     * @return 返回条件句式实例。
     */
    public static Conditional createEqualTo(String fieldName, LiteralBase literalBase, Object value) {
        return Conditional.createEqualTo(new StorageField(fieldName, literalBase, value));
    }

    /**
     * 创建等于运算。
     *
     * @param field 字段描述。
     * @return 返回条件句式实例。
     */
    public static Conditional createEqualTo(StorageField field) {
        String value = renderValue(field);

        String table = field.getTableName();
        if (null != table) {
            return new Conditional(Quote + table + Quote + "." + Quote + field.getName() + Quote + "=" + value,
                    true);
        }
        else {
            return new Conditional(Quote + field.getName() + Quote + "=" + value,
                    true);
        }
    }

    /**
     * 创建等于 JOIN 。
     *
     * @param leftJoinField 左侧连接字段。
     * @param rightJoinField 右侧连接字段。
     * @return 返回条件句式实例。
     */
    public static Conditional createEqualTo(StorageField leftJoinField, StorageField rightJoinField) {
        StringBuilder buf = new StringBuilder();
        buf.append(Quote).append(leftJoinField.getTableName()).append(Quote + "." + Quote).append(leftJoinField.getName()).append(Quote);
        buf.append("=");
        buf.append(Quote).append(rightJoinField.getTableName()).append(Quote + "." + Quote).append(rightJoinField.getName()).append(Quote);
        return new Conditional(buf.toString(), true);
    }

    /**
     * 创建不等于运算。
     *
     * @param fieldName 字段名。
     * @param value 长整型字段值。
     * @return 返回条件句式实例。
     */
    public static Conditional createUnequalTo(String fieldName, long value) {
        StringBuilder buf = new StringBuilder();
        buf.append(Quote).append(fieldName).append(Quote);
        buf.append("<>");
        buf.append(value);
        return new Conditional(buf.toString(), true);
    }

    /**
     * 创建不等于运算。
     *
     * @param field 字段描述。
     * @return
     */
    public static Conditional createUnequalTo(StorageField field) {
        String value = renderValue(field);

        String table = field.getTableName();
        if (null != table) {
            return new Conditional(Quote + table + Quote + "." + Quote + field.getName() + Quote + "<>" + value,
                    true);
        }
        else {
            return new Conditional(Quote + field.getName() + Quote + "<>" + value,
                    true);
        }
    }

    /**
     * 创建大于运算。
     *
     * @param field 字段描述。
     * @return 返回条件句式实例。
     */
    public static Conditional createGreaterThan(StorageField field) {
        return new Conditional(Quote + field.getName() + Quote + ">" + renderValue(field),
                true);
    }

    /**
     * 创建大于等于运算。
     *
     * @param field 字段描述。
     * @return 返回条件句式实例。
     */
    public static Conditional createGreaterThanEqual(StorageField field) {
        return new Conditional(Quote + field.getName() + Quote + ">=" + renderValue(field),
                true);
    }

    /**
     * 创建小于运算。
     *
     * @param field 字段描述。
     * @return 返回条件句式实例。
     */
    public static Conditional createLessThan(StorageField field) {
        return new Conditional(Quote + field.getName() + Quote + "<" + renderValue(field),
                true);
    }

    /**
     * 创建小于等于运算。
     *
     * @param field 字段描述
     * @return 返回条件句式实例。
     */
    public static Conditional createLessThanEqual(StorageField field) {
        return new Conditional(Quote + field.getName() + Quote + "<=" + renderValue(field),
                true);
    }

    /**
     * 创建 column IN (value1, value2, ...) 条件。
     *
     * @param field 字段描述
     * @param values 对应的值数组。
     * @return 返回条件句式实例。
     * @throws IllegalArgumentException 值数组为空或包含 {@code null} 元素时抛出。
     */
    public static Conditional createIN(StorageField field, Object[] values) {
        if (null == values || values.length == 0) {
            throw new IllegalArgumentException("#createIN - Empty values, refuse to build SQL for field: " + field.getName());
        }

        LiteralBase literal = field.getLiteralBase();

        StringBuilder buf = new StringBuilder();
        buf.append(Quote).append(field.getName()).append(Quote);
        buf.append(" IN (");

        for (int i = 0; i < values.length; ++i) {
            Object value = values[i];
            if (null == value) {
                throw new IllegalArgumentException("#createIN - Null element in values, refuse to build SQL for field: "
                        + field.getName());
            }

            if (i > 0) {
                buf.append(",");
            }

            if (null == literal) {
                buf.append("'").append(SQLUtils.correctString(value.toString())).append("'");
                continue;
            }

            switch (literal) {
                case LONG:
                    buf.append(((Long) value).longValue());
                    break;
                case INT:
                    buf.append(((Integer) value).intValue());
                    break;
                case BOOL:
                    buf.append(((Boolean) value).booleanValue() ? "1" : "0");
                    break;
                case STRING:
                default:
                    // 历史实现对未覆盖的类型输出 "0"，会静默改变匹配结果，这里统一按字符串转义处理
                    buf.append("'").append(SQLUtils.correctString(value.toString())).append("'");
                    break;
            }
        }

        buf.append(")");
        return new Conditional(buf.toString());
    }

    /**
     * 创建 LIKE %value% 条件。
     *
     * <p>关键字中的 {@code %} 、{@code _} 与 {@code \} 会被转义，
     * 并通过 {@code ESCAPE} 子句显式声明转义字符，确保用户输入只作为普通字符参与匹配。</p>
     *
     * @param fieldName 字段名。
     * @param keyword 匹配关键字。
     * @return 返回条件句式实例。
     */
    public static Conditional createLike(String fieldName, String keyword) {
        StringBuilder buf = new StringBuilder();
        buf.append(Quote).append(fieldName).append(Quote);
        buf.append(" LIKE '%").append(escapeLikePattern(keyword)).append("%'");
        // 显式声明转义字符，避免用户输入被当作通配符
        buf.append(" ESCAPE '").append(LIKE_ESCAPE_CHAR).append(LIKE_ESCAPE_CHAR).append("'");
        return new Conditional(buf.toString(), true);
    }

    /**
     * 创建 ORDER BY 条件。
     *
     * @param fieldName 字段名。
     * @param desc 是否倒序。
     * @return 返回条件句式实例。
     */
    public static Conditional createOrderBy(String fieldName, boolean desc) {
        StringBuilder buf = new StringBuilder();
        buf.append("ORDER BY ").append(Quote).append(fieldName).append(Quote);
        if (desc) {
            buf.append(" DESC");
        }
        return new Conditional(buf.toString());
    }

    /**
     * 创建 ORDER BY 条件。
     *
     * @param tableName 表名。
     * @param fieldName 字段名。
     * @param desc 是否倒序。
     * @return 返回条件句式实例。
     */
    public static Conditional createOrderBy(String tableName, String fieldName, boolean desc) {
        StringBuilder buf = new StringBuilder();
        buf.append("ORDER BY ");
        buf.append(Quote).append(tableName).append(Quote).append(".");
        buf.append(Quote).append(fieldName).append(Quote);
        if (desc) {
            buf.append(" DESC");
        }
        return new Conditional(buf.toString());
    }

    /**
     * 创建 LIMIT %n OFFSET %n 条件。
     *
     * @param limit 限制量。
     * @param offset 偏移量。
     * @return
     */
    public static Conditional createLimitOffset(int limit, int offset) {
        StringBuilder buf = new StringBuilder();
        buf.append("LIMIT ").append(limit);
        buf.append(" OFFSET ").append(offset);
        return new Conditional(buf.toString());
    }
}
