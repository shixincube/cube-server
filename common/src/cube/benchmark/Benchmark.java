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

    private Map<String, Map<String, List<ResponseTime>>> responseTimeMap;

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
            JSONObject map = responseTimeMapJson.getJSONObject(name);

            Map<String, List<ResponseTime>> timeMap = new HashMap<>();

            Iterator<String> mapiter = map.keys();
            while (mapiter.hasNext()) {
                String action = mapiter.next();
                JSONArray array = map.getJSONArray(action);

                List<ResponseTime> list = new LinkedList<>();
                for (int i = 0; i < array.length(); ++i) {
                    JSONObject timeJson = array.getJSONObject(i);
                    ResponseTime time = new ResponseTime(timeJson);
                    list.add(time);
                }

                timeMap.put(action, list);
            }

            this.responseTimeMap.put(name, timeMap);
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
     * @param timeMap
     */
    public void addResponseTimes(String name, Map<String, List<ResponseTime>> timeMap) {
        Map<String, List<ResponseTime>> newMap = new HashMap<>();

        Iterator<Map.Entry<String, List<ResponseTime>>> iter = timeMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, List<ResponseTime>> e = iter.next();

            String key = e.getKey();
            List<ResponseTime> value = e.getValue();

            List<ResponseTime> list = new ArrayList<>(value.size());
            for (ResponseTime rt : value) {
                if (rt.ending == 0) {
                    // 跳过无效的数据
                    continue;
                }

                list.add(rt);
            }
            newMap.put(key, list);
        }

        this.responseTimeMap.put(name, newMap);
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

        Map<String, List<ResponseTime>> map = this.responseTimeMap.get(name);
        if (null == map) {
            return result;
        }

        Iterator<Map.Entry<String, List<ResponseTime>>> iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, List<ResponseTime>> e = iter.next();
            String action = e.getKey();

            AverageValue value = new AverageValue();
            result.put(action, value);

            List<ResponseTime> list = e.getValue();

            for (ResponseTime time : list) {
                if (0 == time.ending) {
                    continue;
                }

                long duration = time.ending - time.beginning;
                value.durations.add(duration);
                value.total += duration;
                value.count += 1;
            }
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

        JSONObject responseTimeMap = new JSONObject();
        Iterator<Map.Entry<String, Map<String, List<ResponseTime>>>> iter = this.responseTimeMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Map<String, List<ResponseTime>>> e = iter.next();

            String key = e.getKey();
            Map<String, List<ResponseTime>> value = e.getValue();

            JSONObject mapJson = JSONUtils.toJSONObjectAsList(value);

            responseTimeMap.put(key, mapJson);
        }
        json.put("responseTimeMap", responseTimeMap);

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

    public JSONObject toDetailJSON() {
        JSONObject json = this.toJSON();

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
