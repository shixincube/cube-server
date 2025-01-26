/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.cache;

import cell.adapter.extra.timeseries.SeriesItem;
import cell.adapter.extra.timeseries.SeriesMemory;
import cell.adapter.extra.timeseries.SeriesMemoryConfig;
import cube.core.AbstractTimeSeriesCache;
import cube.core.CacheKey;
import cube.core.CacheValue;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 时序缓存。
 */
public class SeriesMemoryTimeSeriesCache extends AbstractTimeSeriesCache {

    public final static String TYPE = "SMTSC";

    private String configFile;

    private SeriesMemory memory;

    /**
     * 构造函数。
     *
     * @param name 缓存器名称。
     */
    public SeriesMemoryTimeSeriesCache(String name) {
        super(name, TYPE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() {
        if (null != this.memory) {
            return;
        }

        SeriesMemoryConfig config = new SeriesMemoryConfig(this.configFile);
        this.memory = new SeriesMemory(config);

        this.memory.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (null == this.memory) {
            return;
        }

        this.memory.stop();
        this.memory = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(JSONObject config) {
        super.configure(config);

        try {
            this.configFile = config.getString("configFile");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(CacheKey key, CacheValue value) {
        long timestamp = value.getTimestamp();
        if (timestamp == 0) {
            this.memory.add(key.get(), value.get());
        }
        else {
            this.memory.add(key.get(), value.get(), timestamp);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(CacheKey key, CacheValue value, long timestamp) {
        this.memory.add(key.get(), value.get(), timestamp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CacheValue> query(CacheKey key, long beginningTime, long endingTime) {
        List<SeriesItem> list = this.memory.query(key.get(), beginningTime, endingTime);
        ArrayList<CacheValue> result = new ArrayList<>();
        for (SeriesItem item : list) {
            CacheValue value = new CacheValue(item.data, item.timestamp);
            result.add(value);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(CacheKey key, long timestamp) {
        this.memory.delete(key.get(), timestamp);
    }
}
