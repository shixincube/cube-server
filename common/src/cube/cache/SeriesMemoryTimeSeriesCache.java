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
