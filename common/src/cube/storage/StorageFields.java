/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
