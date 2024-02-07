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
