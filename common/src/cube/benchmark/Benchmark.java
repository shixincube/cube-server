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

package cube.benchmark;

import java.util.*;

/**
 * 基准。
 */
public class Benchmark {

    private HashMap<String, Long> counterMap;

    private HashMap<String, Queue<ResponseTime>> responseTimeMap;

    public Benchmark() {
        this.counterMap = new HashMap<>();
        this.responseTimeMap = new HashMap<>();
    }

    public void addCounter(String name, Long counter) {
        if (this.counterMap.containsKey(name)) {
            this.counterMap.remove(name);
        }
        this.counterMap.put(name, counter);
    }

    public void addResponseTimes(String name, Queue<ResponseTime> list) {
        Queue<ResponseTime> queue = this.responseTimeMap.get(name);
        if (null == queue) {
            queue = new LinkedList<>();
            this.responseTimeMap.put(name, queue);
        }
        queue.addAll(list);
    }

    public Map<String, AverageValue> calcAverageResponseTime(String name) {
        Map<String, AverageValue> result = new HashMap<>();

        Queue<ResponseTime> queue = this.responseTimeMap.get(name);
        if (null == queue) {
            return result;
        }

        Iterator<ResponseTime> iter = queue.iterator();
        while (iter.hasNext()) {
            ResponseTime time = iter.next();
            if (null == time.mark) {
                continue;
            }

            AverageValue value = result.get(time.mark);
            if (null == value) {
                value = new AverageValue();
                result.put(time.mark, value);
            }
            value.total += time.ending - time.beginning;
            value.count += 1;
        }

        Iterator<AverageValue> viter = result.values().iterator();
        while (viter.hasNext()) {
            AverageValue value = viter.next();
            if (value.count == 0) {
                continue;
            }

            value.value = Math.round(value.total / value.count);
        }

        return result;
    }

    /**
     * 平均值。
     */
    public class AverageValue {

        protected long total = 0;

        protected long count = 0;

        public long value = 0;

        protected AverageValue() {
        }
    }
}
