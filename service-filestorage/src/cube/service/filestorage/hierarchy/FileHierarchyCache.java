/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

package cube.service.filestorage.hierarchy;

import cube.common.UniqueKey;
import cube.core.*;
import cube.service.filestorage.ServiceStorage;
import org.json.JSONObject;

/**
 * 使用数据库存储实现的缓存。
 */
public class FileHierarchyCache extends AbstractCache {

    protected ServiceStorage structStorage;

    public FileHierarchyCache(ServiceStorage structStorage) {
        super("FileHierarchyCache", "FileHierarchyCache");
        this.structStorage = structStorage;
    }

    @Override
    public void start() {
        // Nothing
    }

    @Override
    public void stop() {
        // Nothing
    }

    @Override
    public void put(CacheKey key, CacheValue value) {
        Object[] uk = UniqueKey.extract(key.get());
        Long id = (Long) uk[0];
        String domain = (String) uk[1];
        this.structStorage.writeHierarchyNode(domain, id, value.get());
    }

    @Override
    public CacheValue get(CacheKey key) {
        Object[] uk = UniqueKey.extract(key.get());
        Long id = (Long) uk[0];
        String domain = (String) uk[1];

        JSONObject json = this.structStorage.readHierarchyNode(domain, id);
        if (null == json) {
            return null;
        }

        CacheValue value = new CacheValue(json);
        return value;
    }

    @Override
    public void remove(CacheKey key) {
        Object[] uk = UniqueKey.extract(key.get());
        Long id = (Long) uk[0];
        String domain = (String) uk[1];
        this.structStorage.deleteHierarchyNode(domain, id);
    }

    @Override
    public CacheValue get(CacheExpression expression) {
        // Nothing
        return null;
    }

    @Override
    public void execute(CacheKey key, CacheTransaction transaction) {
        // Nothing
    }
}
