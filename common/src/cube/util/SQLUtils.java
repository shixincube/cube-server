/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

package cube.util;

import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.StorageField;

import java.util.List;

/**
 * SQL 辅助函数。
 */
public final class SQLUtils {

    public final static String Quote = "`";

    public final static String FILTER_PATTERN = "[-;'\",\\s]|(--)+";

    private SQLUtils() {
    }

    /**
     * 矫正表名。
     *
     * @param name
     * @return
     */
    public static String correctTableName(String name) {
        String result = name.replaceAll("\\.", "_");
        result = result.replaceAll("-", "_");
        return result;
    }

    /**
     * 矫正 SQL 字符串。
     *
     * @param string
     * @return
     */
    public static String correctString(String string) {
        String result = string.replaceAll("'", "''");
//        result = result.replaceAll("/", "//");
//        result = result.replaceAll("%", "/%");
//        result = result.replaceAll("_", "/_");
        result = result.replaceAll("\\[", "/[");
        result = result.replaceAll("]", "/]");
//        result = result.replaceAll("\\(", "/(");
//        result = result.replaceAll("\\)", "/)");
        return result;
    }

    /**
     * 拼装 SELECT 语句。
     *
     * @param table
     * @param fields
     * @param conditionals
     * @return
     */
    public static String spellSelect(String table, StorageField[] fields, Conditional[] conditionals) {
        StringBuilder buf = new StringBuilder("SELECT ");

        if (null != fields) {
            for (StorageField field : fields) {
                buf.append(Quote).append(field.getName()).append(Quote);
                buf.append(",");
            }
            buf.delete(buf.length() - 1, buf.length());
        }
        else {
            buf.append("*");
        }

        buf.append(" FROM ");
        buf.append(Quote);
        buf.append(table);
        buf.append(Quote);

        if (null != conditionals) {
            if (conditionals[0].needWhereSentence()) {
                buf.append(" WHERE ");
            }
            else {
                buf.append(" ");
            }

            for (Conditional cond : conditionals) {
                if (null == cond) {
                    continue;
                }

                buf.append(cond.toString()).append(" ");
            }
        }

        return buf.toString();
    }

    /**
     * 拼装 SELECT 语句。
     *
     * @param tables
     * @param fields
     * @param conditionals
     * @return
     */
    public static String spellSelect(String[] tables, StorageField[] fields, Conditional[] conditionals) {
        StringBuilder buf = new StringBuilder("SELECT ");

        if (null != fields) {
            for (StorageField field : fields) {
                buf.append(Quote).append(field.getTableName()).append(Quote)
                        .append(".").append(Quote).append(field.getName()).append(Quote);
                buf.append(",");
            }
            buf.delete(buf.length() - 1, buf.length());
        }
        else {
            buf.append("*");
        }

        buf.append(" FROM ");
        for (String table : tables) {
            buf.append(table);
            buf.append(",");
        }
        buf.delete(buf.length() - 1, buf.length());

        if (null != conditionals) {
            buf.append(" WHERE ");
            for (Conditional cond : conditionals) {
                if (null == cond) {
                    continue;
                }

                buf.append(cond.toString()).append(" ");
            }
        }

        return buf.toString();
    }

    /**
     * 拼装 CREATE TABLE 语句。
     *
     * @param table
     * @param fields
     * @return
     */
    public static String spellCreateTable(String table, StorageField[] fields) {
        StringBuilder buf = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        buf.append(table);
        buf.append(" (");
        for (StorageField field : fields) {
            // 字段名
            buf.append(Quote).append(field.getName()).append(Quote);

            switch (field.getLiteralBase()) {
                case STRING:
                    buf.append(" TEXT ");
                    break;
                case INT:
                    buf.append(" INTEGER ");
                    break;
                case LONG:
                    buf.append(" BIGINT ");
                    break;
                case BOOL:
                    buf.append(" BOOLEAN ");
                    break;
                default:
                    break;
            }

            Constraint[] constraints = field.getConstraints();
            if (null != constraints) {
                for (Constraint constraint : constraints) {
                    buf.append(constraint.getStatement()).append(" ");
                }
            }

            buf.append(",");
        }

        // 修正逗号
        buf.delete(buf.length() - 1, buf.length());

        buf.append(")");

        return buf.toString();
    }

    /**
     * 拼装 INSERT 语句。
     *
     * @param table
     * @param fields
     * @return
     */
    public static String spellInsert(String table, StorageField[] fields) {
        StringBuilder buf = new StringBuilder("INSERT INTO ");
        buf.append(table);
        buf.append(" (");
        for (StorageField field : fields) {
            if (null == field.getValue()) {
                // 跳过空值
                continue;
            }

            buf.append(Quote).append(field.getName()).append(Quote);
            buf.append(",");
        }
        // 修正逗号
        buf.delete(buf.length() - 1, buf.length());
        buf.append(") VALUES (");

        for (StorageField field : fields) {
            if (null == field.getValue()) {
                // 跳过空值
                continue;
            }

            switch (field.getLiteralBase()) {
                case STRING:
                    buf.append("'").append(SQLUtils.correctString(field.getString())).append("'");
                    break;
                case INT:
                    buf.append(field.getInt());
                    break;
                case LONG:
                    buf.append(field.getLong());
                    break;
                case BOOL:
                    buf.append(field.getBoolean() ? 1 : 0);
                    break;
                default:
                    break;
            }
            buf.append(",");
        }
        // 修正逗号
        buf.delete(buf.length() - 1, buf.length());

        buf.append(")");

        return buf.toString();
    }

    /**
     * 拼装 UPDATE 语句。
     *
     * @param table
     * @param fields
     * @param conditionals
     * @return
     */
    public static String spellUpdate(String table, StorageField[] fields, Conditional[] conditionals) {
        StringBuilder buf = new StringBuilder("UPDATE ");
        buf.append(table);
        buf.append(" SET ");

        for (StorageField field : fields) {
            if (null == field.getValue()) {
                // 跳过空值
                continue;
            }

            buf.append(Quote).append(field.getName()).append(Quote).append("=");

            switch (field.getLiteralBase()) {
                case STRING:
                    buf.append("'").append(SQLUtils.correctString(field.getString())).append("'");
                    break;
                case INT:
                    buf.append(field.getInt());
                    break;
                case LONG:
                    buf.append(field.getLong());
                    break;
                case BOOL:
                    buf.append(field.getBoolean() ? 1 : 0);
                    break;
                default:
                    break;
            }
            buf.append(",");
        }
        // 修正逗号
        buf.delete(buf.length() - 1, buf.length());

        buf.append(" WHERE ");
        for (Conditional conditional : conditionals) {
            if (null == conditional) {
                // 跳过 null 值
                continue;
            }

            buf.append(conditional.toString());
            buf.append(" ");
        }

        return buf.toString();
    }

    /**
     * 拼装 DELETE 语句。
     *
     * @param table
     * @param conditionals
     * @return
     */
    public static String spellDelete(String table, Conditional[] conditionals) {
        StringBuilder buf = new StringBuilder("DELETE FROM ");
        buf.append(table);
        buf.append(" WHERE ");
        for (Conditional conditional : conditionals) {
            if (null == conditional) {
                continue;
            }

            buf.append(conditional.toString());
            buf.append(" ");
        }

        return buf.toString();
    }
}
