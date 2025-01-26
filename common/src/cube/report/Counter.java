/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.report;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 简易的计数器。
 */
public class Counter {

    private ConcurrentHashMap<String, CounterRecord> records;

    public Counter() {
        this.records = new ConcurrentHashMap<>();
    }

    public class CounterRecord {

        private List<Long> timestamps;

        private AtomicInteger counts;

        protected CounterRecord() {
            this.timestamps = new Vector<>();
            this.counts = new AtomicInteger(0);
        }
    }
}
