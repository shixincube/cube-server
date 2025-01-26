/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.filestorage.hierarchy;

import cell.util.log.Logger;
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

        try {
            JSONObject json = this.structStorage.readHierarchyNode(domain, id);
            if (null == json) {
                return null;
            }

            CacheValue value = new CacheValue(json);
            return value;
        } catch (Exception e) {
            Logger.e(this.getClass(), "#get", e);
            return null;
        }
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
