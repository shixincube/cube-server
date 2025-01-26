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
    public String getName();

    /**
     * 获取配置。
     *
     * @return
     */
    public JSONObject getConfig();

    /**
     * 获取类型。
     *
     * @return
     */
    public StorageType getType();

    /**
     *
     * @param config
     */
    public void configure(JSONObject config);

    /**
     * 打开存储仓库。
     */
    public void open();

    /**
     * 关闭存储仓库。
     */
    public void close();

    public boolean exist(String table);

    public boolean executeCreate(String table, StorageField[] fields);

    public boolean executeInsert(String table, StorageField[] fields);

    public boolean executeInsert(String table, List<StorageField[]> fieldsList);

    public boolean executeUpdate(String table, StorageField[] fields, Conditional[] conditionals);

    public boolean executeDelete(String table, Conditional[] conditionals);

    public List<StorageField[]> executeQuery(String table, StorageField[] fields);

    public List<StorageField[]> executeQuery(String table, StorageField[] fields, Conditional[] conditionals);

    public List<StorageField[]> executeQuery(String[] tables, StorageField[] fields, Conditional[] conditionals);

    public List<StorageField[]> executeQuery(String sql);

    public boolean execute(String sql);
}
