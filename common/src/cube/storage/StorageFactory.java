/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.storage;

import cube.core.Storage;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 存储工厂。
 */
public final class StorageFactory {

    public final static String SQLITE_FILE = SQLiteStorage.CONFIG_FILE;

    public final static String MYSQL_HOST = MySQLStorage.CONFIG_HOST;
    public final static String MYSQL_PORT = MySQLStorage.CONFIG_PORT;
    public final static String MYSQL_SCHEMA = MySQLStorage.CONFIG_SCHEMA;
    public final static String MYSQL_USER = MySQLStorage.CONFIG_USER;
    public final static String MYSQL_PASSWORD = MySQLStorage.CONFIG_PASSWORD;

    private final static StorageFactory instance = new StorageFactory();

    private ConcurrentHashMap<String, Storage> storageMap;

    private StorageFactory() {
        this.storageMap = new ConcurrentHashMap<>();
    }

    public static StorageFactory getInstance() {
        return StorageFactory.instance;
    }

    /**
     * 创构建存储。
     *
     * @param type 存储类型。
     * @param name 存储名称。
     * @param config 存储的配置。
     * @return 返回存储实例。
     */
    public Storage createStorage(StorageType type, String name, JSONObject config) {
        Storage storage = null;

        if (type == StorageType.SQLite) {
            storage = new SQLiteStorage(name);
        }
        else if (type == StorageType.MySQL) {
            storage = new MySQLStorage(name);
        }

        if (null != storage) {
            storage.configure(config);
            this.storageMap.put(name, storage);
        }

        return storage;
    }

    /**
     * 获取指定名称的存储。
     *
     * @param name 指定存储器名称。
     * @return 返回存储器实例。
     */
    public Storage getStorage(String name) {
        return this.storageMap.get(name);
    }

    /**
     * 获取指定存储的类型。
     *
     * @param storage 指定存储器。
     * @return 返回存储器类型。
     */
    public StorageType getStorageType(Storage storage) {
        if (storage instanceof  MySQLStorage) {
            return StorageType.MySQL;
        }
        else if (storage instanceof SQLiteStorage) {
            return StorageType.SQLite;
        }
        else {
            return StorageType.Other;
        }
    }
}
