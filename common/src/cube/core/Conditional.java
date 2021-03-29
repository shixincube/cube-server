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
import cube.util.SQLUtils;

/**
 * 条件句式。
 */
public class Conditional {

    public final static String Quote = "`";

    /**
     * 句式的 SQL 语句。
     */
    private String sql;

    /**
     * 构造函数。
     *
     * @param sql 指定 SQL 字符串。
     */
    protected Conditional(String sql) {
        this.sql = sql;
    }

    @Override
    public String toString() {
        return this.sql;
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
        StringBuilder buf = new StringBuilder("( ");
        for (Conditional cond : conditionals) {
            buf.append(cond.sql).append(" ");
        }
        buf.append(")");
        return new Conditional(buf.toString());
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
        String value = field.getValue().toString();

        if (field.getLiteralBase() == LiteralBase.BOOL) {
            value = (field.getBoolean() ? "1" : "0");
        }
        else if (field.getLiteralBase() == LiteralBase.STRING) {
            value = "'" + value + "'";
        }

        String table = field.getTableName();
        if (null != table) {
            return new Conditional(Quote + table + Quote + "." + Quote + field.getName() + Quote + "=" + value);
        }
        else {
            return new Conditional(Quote + field.getName() + Quote + "=" + value);
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
        return new Conditional(buf.toString());
    }

    /**
     * 创建大于运算。
     *
     * @param field 字段描述。
     * @return 返回条件句式实例。
     */
    public static Conditional createGreaterThan(StorageField field) {
        return new Conditional(Quote + field.getName() + Quote + ">" + field.getValue().toString());
    }

    /**
     * 创建大于等于运算。
     *
     * @param field 字段描述。
     * @return 返回条件句式实例。
     */
    public static Conditional createGreaterThanEqual(StorageField field) {
        return new Conditional(Quote + field.getName() + Quote + ">=" + field.getValue().toString());
    }

    /**
     * 创建小于运算。
     *
     * @param field 字段描述。
     * @return 返回条件句式实例。
     */
    public static Conditional createLessThan(StorageField field) {
        return new Conditional(Quote + field.getName() + Quote + "<" + field.getValue().toString());
    }

    /**
     * 创建小于等于运算。
     *
     * @param field 字段描述
     * @return 返回条件句式实例。
     */
    public static Conditional createLessThanEqual(StorageField field) {
        return new Conditional(Quote + field.getName() + Quote + "<=" + field.getValue().toString());
    }

    /**
     * 创建 column IN (value1, value2, ...) 条件。
     *
     * @param field 字段描述
     * @param values 对应的值数组。
     * @return 返回条件句式实例。
     */
    public static Conditional createIN(StorageField field, Object[] values) {
        StringBuilder buf = new StringBuilder();
        buf.append(Quote).append(field.getName()).append(Quote);
        buf.append(" IN (");
        for (Object value : values) {
            switch (field.getLiteralBase()) {
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
                    buf.append("'").append(SQLUtils.correctString(value.toString())).append("'");
                    break;
                default:
                    buf.append("0");
                    break;
            }
            buf.append(",");
        }
        // 修正逗号
        buf.delete(buf.length() - 1, buf.length());

        buf.append(")");
        return new Conditional(buf.toString());
    }

    /**
     * 创建 LIKE %value% 条件。
     *
     * @param fieldName 字段名。
     * @param keyword 匹配关键字。
     * @return 返回条件句式实例。
     */
    public static Conditional createLike(String fieldName, String keyword) {
        StringBuilder buf = new StringBuilder();
        buf.append(Quote).append(fieldName).append(Quote).append(" LIKE '%").append(keyword).append("%'");
        return new Conditional(buf.toString());
    }
}
