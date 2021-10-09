/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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
