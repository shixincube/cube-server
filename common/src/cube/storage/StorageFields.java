/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.storage;

import cell.core.talk.LiteralBase;
import cube.core.StorageField;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 存储字段的扩展方法。
 */
public final class StorageFields {

    private StorageFields() {
    }

    /**
     * 按照字段名称获取对应的映射。
     *
     * @param fields
     * @return
     */
    public static Map<String, StorageField> get(StorageField[] fields) {
        HashMap<String, StorageField> result = new HashMap<>();
        for (StorageField field : fields) {
            result.put(field.getName(), field);
        }
        return result;
    }

    /**
     * 创建 SELECT 语句的 '*' 检索。
     *
     * @return
     */
    public static StorageField[] all() {
        return null;
    }

    /**
     * 搜索 ResultSet 数据，生成 StorageField 数组。
     *
     * @param resultSet
     * @return
     * @throws SQLException
     */
    public static StorageField[] scanResultSet(ResultSet resultSet) throws SQLException {
        ResultSetMetaData md = resultSet.getMetaData();
        int numCol = md.getColumnCount();

        StorageField[] row = new StorageField[numCol];

        for (int i = 1; i <= numCol; ++i) {
            String colName = md.getColumnName(i);
            Object value = resultSet.getObject(i);

            LiteralBase literal = LiteralBase.STRING;
            if (value instanceof Long) {
                literal = LiteralBase.LONG;
            }
            else if (value instanceof Integer) {
                literal = LiteralBase.INT;
            }

            row[i - 1] = new StorageField(colName, literal, value);
        }

        return row;
    }
}
