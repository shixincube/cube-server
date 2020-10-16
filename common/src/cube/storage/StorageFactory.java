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

package cube.storage;

import cell.util.json.JSONObject;
import cube.core.Storage;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 存储工厂。
 */
public final class StorageFactory {

    private final static StorageFactory instance = new StorageFactory();

    private ConcurrentHashMap<String, Storage> storageMap;

    private StorageFactory() {
        this.storageMap = new ConcurrentHashMap<>();
    }

    public static StorageFactory getInstance() {
        return StorageFactory.instance;
    }

    public Storage createStorage(StorageType type, String name, JSONObject config) {
        Storage storage = null;

        if (type == StorageType.SQLite) {
            storage = new SQLiteStorage(name);
        }

        if (null != storage) {
            storage.configure(config);
            this.storageMap.put(name, storage);
        }

        return storage;
    }

    public Storage getStorage(String name) {
        return this.storageMap.get(name);
    }
}
