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

import cube.common.JSONable;
import cube.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * 基准。
 */
public class Benchmark implements JSONable {

    private Map<String, Long> counterMap;

    private Map<String, List<JSONable>> responseTimeMap;

    public Benchmark() {
        this.counterMap = new HashMap<>();
        this.responseTimeMap = new HashMap<>();
    }

    public Benchmark(JSONObject json) {
        JSONObject counterMapJson = json.getJSONObject("counterMap");
        this.counterMap = JSONUtils.toLongMap(counterMapJson);

        this.responseTimeMap = new HashMap<>();
        JSONObject responseTimeMapJson = json.getJSONObject("responseTimeMap");
        Iterator<String> iter = responseTimeMapJson.keys();
        while (iter.hasNext()) {
            String name = iter.next();
            JSONArray array = responseTimeMapJson.getJSONArray(name);
            List<JSONable> list = new LinkedList<>();
            for (int i = 0; i < array.length(); ++i) {
                JSONObject timeJson = array.getJSONObject(i);
                ResponseTime time = new ResponseTime(timeJson);
                list.add(time);
            }

            this.responseTimeMap.put(name, list);
        }
    }

    /**
     * 添加指定名称的计数。
     *
     * @param name
     * @param counter
     */
    public void addCounter(String name, Long counter) {
        if (this.counterMap.containsKey(name)) {
            this.counterMap.remove(name);
        }
        this.counterMap.put(name, counter);
    }

    public Map<String, Long> getCounterMap() {
        return this.counterMap;
    }

    /**
     * 添加应答时间。
     *
     * @param name
     * @param timeList
     */
    public void addResponseTimes(String name, Queue<ResponseTime> timeList) {
        List<JSONable> list = this.responseTimeMap.get(name);
        if (null == list) {
            list = new LinkedList<>();
            this.responseTimeMap.put(name, list);
        }
        list.addAll(timeList);
    }

    public Set<String> getResponseTimeKeys() {
        return this.responseTimeMap.keySet();
    }

    /**
     * 计算应答时间的平均值。
     *
     * @param name
     * @return
     */
    public Map<String, AverageValue> calcAverageResponseTime(String name) {
        Map<String, AverageValue> result = new HashMap<>();

        List<JSONable> list = this.responseTimeMap.get(name);
        if (null == list) {
            return result;
        }

        Iterator<JSONable> iter = list.iterator();
        while (iter.hasNext()) {
            ResponseTime time = (ResponseTime) iter.next();
            if (null == time.mark) {
                continue;
            }

            AverageValue value = result.get(time.mark);
            if (null == value) {
                value = new AverageValue();
                result.put(time.mark, value);
            }

            long duration = time.ending - time.beginning;
            value.durations.add(duration);
            value.total += duration;
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

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("counterMap", JSONUtils.toJSONObjectAsLong(this.counterMap));
        json.put("responseTimeMap", JSONUtils.toJSONObjectAsList(this.responseTimeMap));
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("counterMap", JSONUtils.toJSONObjectAsLong(this.counterMap));

        JSONObject responseTimeMap = new JSONObject();
        Iterator<String> iter = this.responseTimeMap.keySet().iterator();
        while (iter.hasNext()) {
            String name = iter.next();

            Map<String, AverageValue> value = this.calcAverageResponseTime(name);
            JSONObject valueMap = JSONUtils.toJSONObject(value);
            responseTimeMap.put(name, valueMap);
        }
        json.put("avgResponseTimeMap", responseTimeMap);

        return json;
    }

    /**
     * 平均值。
     */
    public class AverageValue implements JSONable {

        protected long total = 0;

        protected long count = 0;

        protected List<Long> durations = new ArrayList<>();

        public long value = 0;

        protected AverageValue() {
        }

        private long getDelta() {
            int size = this.durations.size();
            if (size <= 1) {
                return 0;
            }

            return (this.durations.get(size - 1) - this.durations.get(0));
        }

        @Override
        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("total", this.total);
            json.put("count", this.count);
            json.put("value", this.value);
            json.put("delta", this.getDelta());
            return json;
        }

        @Override
        public JSONObject toCompactJSON() {
            return this.toJSON();
        }
    }
}
