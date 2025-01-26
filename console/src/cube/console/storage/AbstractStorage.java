/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.storage;

import cube.core.Storage;
import cube.storage.StorageFactory;
import cube.storage.StorageType;
import org.json.JSONObject;

import java.util.Properties;

/**
 * 控制器的存储器。
 */
public abstract class AbstractStorage {

    protected Storage storage;

    public AbstractStorage(String name, Properties properties) {
        String dbType = properties.getProperty("db");

        if (dbType.equalsIgnoreCase("mysql")) {
            JSONObject config = new JSONObject();
            config.put(StorageFactory.MYSQL_HOST, properties.getProperty("mysql.host"));
            config.put(StorageFactory.MYSQL_PORT, properties.getProperty("mysql.port"));
            config.put(StorageFactory.MYSQL_SCHEMA, properties.getProperty("mysql.schema"));
            config.put(StorageFactory.MYSQL_USER, properties.getProperty("mysql.user"));
            config.put(StorageFactory.MYSQL_PASSWORD, properties.getProperty("mysql.password"));
            this.storage = StorageFactory.getInstance().createStorage(StorageType.MySQL, name, config);
        }
    }
}
