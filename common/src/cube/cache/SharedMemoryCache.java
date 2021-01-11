/**
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

package cube.cache;

import cell.adapter.extra.memory.LockFuture;
import cell.adapter.extra.memory.SharedMemory;
import cell.adapter.extra.memory.SharedMemoryConfig;
import cube.core.*;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Shared Memory 缓存。
 */
public class SharedMemoryCache extends AbstractCache {

    public final static String TYPE = "SMC";

    private String configFile;

    private SharedMemory memory;

    /**
     * 构造函数。
     *
     * @param name 缓存器名称。
     */
    public SharedMemoryCache(String name) {
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

        SharedMemoryConfig config = new SharedMemoryConfig(this.configFile);
        this.memory = new SharedMemory(config);

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
    public void put(CacheKey key, CacheValue value) {
        this.memory.applyPut(key.get(), value.get());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CacheValue get(CacheKey key) {
        JSONObject value = this.memory.applyGet(key.get());
        if (null == value) {
            return null;
        }
        return new CacheValue(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CacheValue get(CacheExpression expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(CacheKey key) {
        this.memory.applyRemove(key.get());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(CacheKey key, final CacheTransaction transaction) {
        Context context = new Context(key);

        LockFuture future = new LockFuture() {
            @Override
            public void acquired(String key) {
                transaction.perform(context);
            }
        };

        context.lockFuture = future;

        this.memory.apply(key.get(), future);
    }


    /**
     * 事务上下文实现。
     */
    protected class Context extends TransactionContext {

        protected LockFuture lockFuture;

        public Context(CacheKey key) {
            super(key);
        }

        @Override
        public CacheValue get() {
            JSONObject value = this.lockFuture.get();
            if (null != value) {
                return new CacheValue(value);
            }

            return null;
        }

        @Override
        public void put(CacheValue value) {
            this.lockFuture.put(value.get());
        }

        @Override
        public void remove() {
            this.lockFuture.remove();
        }
    }
}
