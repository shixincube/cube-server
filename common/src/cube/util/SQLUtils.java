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

package cube.util;

import cube.core.Constraint;
import cube.core.StorageField;

import java.nio.charset.Charset;

/**
 * SQL 辅助函数。
 */
public final class SQLUtils {

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
        result = result.replaceAll("/", "//");
        result = result.replaceAll("%", "/%");
        result = result.replaceAll("_", "/_");
        result = result.replaceAll("\\[", "/[");
        result = result.replaceAll("]", "/]");
        result = result.replaceAll("\\(", "/(");
        result = result.replaceAll("\\)", "/)");
        return result;
    }

    /**
     * 拼装 SELECT 语句。
     *
     * @param table
     * @param fields
     * @param conditional
     * @return
     */
    public static String spellSelect(String table, StorageField[] fields, String conditional) {
        StringBuilder buf = new StringBuilder("SELECT ");
        for (StorageField field : fields) {
            buf.append("[").append(field.getName()).append("]");
            buf.append(",");
        }
        buf.delete(buf.length() - 1, buf.length());

        buf.append(" FROM ");
        buf.append(table);
        if (null != conditional && conditional.length() > 2) {
            buf.append(" WHERE ");
            buf.append(conditional);
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
        buf.append(" ( ");
        for (StorageField field : fields) {
            // 字段名
            buf.append("[").append(field.getName()).append("]");

            switch (field.getLiteralBase()) {
                case STRING:
                    buf.append(" TEXT ");
                    break;
                case INT:
                case LONG:
                    buf.append(" INTEGER ");
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

        buf.append(" )");

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
            buf.append("[").append(field.getName()).append("]");
            buf.append(",");
        }
        // 修正逗号
        buf.delete(buf.length() - 1, buf.length());
        buf.append(") VALUES (");

        for (StorageField field : fields) {
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
}
