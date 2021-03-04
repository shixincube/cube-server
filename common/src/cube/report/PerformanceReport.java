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

package cube.report;

import cube.benchmark.Benchmark;
import cube.common.JSONable;
import cube.core.AbstractCellet;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 性能报告。
 */
public class PerformanceReport extends Report {

    public final static String NAME = "PerfReport";

    private Benchmark benchmark;

    private Map<Integer, ConnectionReport> serverConnMap;

    public PerformanceReport(String reporter) {
        super(NAME);
        this.setReporter(reporter);
        this.benchmark = new Benchmark();
        this.serverConnMap = new HashMap<>();
    }

    public PerformanceReport(JSONObject json) throws Exception {
        super(json);

        System.out.println(json.toString());
        this.benchmark = new Benchmark(json.getJSONObject("benchmark"));

        this.serverConnMap = new HashMap<>();
        JSONArray array = json.getJSONArray("connNums");
        for (int i = 0; i < array.length(); ++i) {
            JSONObject value = array.getJSONObject(i);
            ConnectionReport cr = new ConnectionReport(value);
            this.serverConnMap.put(cr.port, cr);
        }
    }

    public void gather(AbstractCellet cellet) {
        this.benchmark.addCounter(cellet.getName(), cellet.getListenedCounter().get());
        this.benchmark.addResponseTimes(cellet.getName(), cellet.getResponseTimes());
    }

    public void reportConnection(int port, int numRealtime, int numMax) {
        this.serverConnMap.put(port, new ConnectionReport(port, numRealtime, numMax));
    }

    public Benchmark getBenchmark() {
        return this.benchmark;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("benchmark", this.benchmark.toJSON());

        JSONArray array = new JSONArray();
        Iterator<ConnectionReport> iter = this.serverConnMap.values().iterator();
        while (iter.hasNext()) {
            array.put(iter.next().toJSON());
        }
        json.put("connNums", array);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toJSON();

        // 仅输出平均值
        json.put("benchmark", this.benchmark.toCompactJSON());

        JSONArray array = new JSONArray();
        Iterator<ConnectionReport> iter = this.serverConnMap.values().iterator();
        while (iter.hasNext()) {
            array.put(iter.next().toJSON());
        }
        json.put("connNums", array);
        return json;
    }


    /**
     * 连接数据。
     */
    public class ConnectionReport implements JSONable {

        public final int port;

        public final int realtimeNum;

        public final int maxNum;

        protected ConnectionReport(int port, int realtimeNum, int maxNum) {
            this.port = port;
            this.realtimeNum = realtimeNum;
            this.maxNum = maxNum;
        }

        protected ConnectionReport(JSONObject json) {
            this.port = json.getInt("port");
            this.realtimeNum = json.getInt("realtime");
            this.maxNum = json.getInt("max");
        }

        @Override
        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("port", this.port);
            json.put("realtime", this.realtimeNum);
            json.put("max", this.maxNum);
            return json;
        }

        @Override
        public JSONObject toCompactJSON() {
            return toJSON();
        }
    }
}
