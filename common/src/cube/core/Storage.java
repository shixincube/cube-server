/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.core;

import cube.storage.StorageType;
import org.json.JSONObject;

import java.util.List;

/**
 * 存储器接口。
 */
public interface Storage {

    /**
     * 获取存储器名称。
     *
     * @return
     */
    String getName();

    /**
     * 获取配置。
     *
     * @return
     */
    JSONObject getConfig();

    /**
     * 获取类型。
     *
     * @return
     */
    StorageType getType();

    /**
     *
     * @param config
     */
    void configure(JSONObject config);

    /**
     * 打开存储仓库。
     */
    void open();

    /**
     * 关闭存储仓库。
     */
    void close();

    boolean exist(String table);

    boolean executeCreate(String table, StorageField[] fields);

    boolean executeInsert(String table, StorageField[] fields);

    boolean executeInsert(String table, List<StorageField[]> fieldsList);

    boolean executeUpdate(String table, StorageField[] fields, Conditional[] conditionals);

    boolean executeDelete(String table, Conditional[] conditionals);

    List<StorageField[]> executeQuery(String table, StorageField[] fields);

    List<StorageField[]> executeQuery(String table, StorageField[] fields, Conditional[] conditionals);

    List<StorageField[]> executeQuery(String[] tables, StorageField[] fields, Conditional[] conditionals);

    List<StorageField[]> executeQuery(String sql);

    boolean execute(String sql);
}
