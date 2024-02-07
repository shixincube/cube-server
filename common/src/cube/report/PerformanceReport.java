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

package cube.report;

import cube.benchmark.Benchmark;
import cube.common.JSONable;
import cube.core.AbstractCellet;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/*
{
    "systemStartTime":1615011551967,
    "name":"PerfReport",
    "connNums":[
        {
            "realtime":1,
            "port":6000,
            "max":1000
        }
    ],
    "reporter":"344e19c671bc9376#service#6000",
    "systemDuration":38438,
    "items":{
        "Contact":{
            "onlineNum":1,
            "maxNum":10000
        }
    },
    "benchmark":{
        "counterMap":{
            "FileProcessor":0,
            "MultipointComm":0,
            "Auth":1,
            "Messaging":1,
            "FileStorage":1,
            "Contact":12
        },
        "responseTimeMap":{
            "FileProcessor":{

            },
            "MultipointComm":{

            },
            "Auth":{
                "getToken":[
                    {
                        "ending":1615011587656,
                        "beginning":1615011587655,
                        "mark":"getToken"
                    }
                ]
            },
            "Messaging":{
                "pull":[
                    {
                        "ending":1615011586023,
                        "beginning":1615011586011,
                        "mark":"pull"
                    }
                ]
            },
            "FileStorage":{
                "getRoot":[
                    {
                        "ending":1615011586098,
                        "beginning":1615011586079,
                        "mark":"getRoot"
                    }
                ]
            },
            "Contact":{
                "signIn":[
                    {
                        "ending":1615011585965,
                        "beginning":1615011585946,
                        "mark":"signIn"
                    }
                ],
                "listGroups":[
                    {
                        "ending":1615011586012,
                        "beginning":1615011586010,
                        "mark":"listGroups"
                    }
                ],
                "getAppendix":[
                    {
                        "ending":1615011586153,
                        "beginning":1615011586148,
                        "mark":"getAppendix"
                    },
                    {
                        "ending":1615011586164,
                        "beginning":1615011586148,
                        "mark":"getAppendix"
                    },
                    {
                        "ending":1615011586164,
                        "beginning":1615011586149,
                        "mark":"getAppendix"
                    },
                    {
                        "ending":1615011586164,
                        "beginning":1615011586149,
                        "mark":"getAppendix"
                    },
                    {
                        "ending":1615011586641,
                        "beginning":1615011586637,
                        "mark":"getAppendix"
                    }
                ],
                "getContact":[
                    {
                        "ending":1615011586101,
                        "beginning":1615011586080,
                        "mark":"getContact"
                    },
                    {
                        "ending":1615011586114,
                        "beginning":1615011586080,
                        "mark":"getContact"
                    },
                    {
                        "ending":1615011586114,
                        "beginning":1615011586081,
                        "mark":"getContact"
                    },
                    {
                        "ending":1615011586114,
                        "beginning":1615011586083,
                        "mark":"getContact"
                    },
                    {
                        "ending":1615011586593,
                        "beginning":1615011586589,
                        "mark":"getContact"
                    }
                ]
            }
        },
        "avgResponseTimeMap":{
            "FileProcessor":{

            },
            "MultipointComm":{

            },
            "Auth":{
                "getToken":{
                    "total":1,
                    "count":1,
                    "delta":0,
                    "value":1
                }
            },
            "Messaging":{
                "pull":{
                    "total":12,
                    "count":1,
                    "delta":0,
                    "value":12
                }
            },
            "FileStorage":{
                "getRoot":{
                    "total":19,
                    "count":1,
                    "delta":0,
                    "value":19
                }
            },
            "Contact":{
                "signIn":{
                    "total":19,
                    "count":1,
                    "delta":0,
                    "value":19
                },
                "listGroups":{
                    "total":2,
                    "count":1,
                    "delta":0,
                    "value":2
                },
                "getAppendix":{
                    "total":55,
                    "count":5,
                    "delta":-1,
                    "value":11
                },
                "getContact":{
                    "total":123,
                    "count":5,
                    "delta":-17,
                    "value":24
                }
            }
        }
    },
    "timestamp":1615011590405
}
*/

/**
 * 性能报告。
 */
public class PerformanceReport extends Report {

    public final static String NAME = "PerfReport";

    /** 基准性能。 */
    private Benchmark benchmark;

    /** 服务器连接数据。 */
    private Map<Integer, ConnectionReport> serverConnMap;

    /** 内核启动时间。 */
    private long systemStartTime;

    /** 截止本次快照生成时内核运行的时长，单位：毫秒。 */
    private long systemDuration;

    /** 其他性能项。 */
    private Map<String, JSONObject> itemMap;

    public PerformanceReport(String reporter, long timestamp) {
        super(NAME, timestamp);
        this.setReporter(reporter);
        this.benchmark = new Benchmark();
        this.serverConnMap = new HashMap<>();
    }

    public PerformanceReport(JSONObject json) throws Exception {
        super(json);

        this.systemStartTime = json.getLong("systemStartTime");
        this.systemDuration = json.getLong("systemDuration");

        this.benchmark = new Benchmark(json.getJSONObject("benchmark"));

        this.serverConnMap = new HashMap<>();
        JSONArray array = json.getJSONArray("connNums");
        for (int i = 0; i < array.length(); ++i) {
            JSONObject value = array.getJSONObject(i);
            ConnectionReport cr = new ConnectionReport(value);
            this.serverConnMap.put(cr.port, cr);
        }

        this.itemMap = new HashMap<>();
        JSONObject items = json.getJSONObject("items");
        Iterator<String> keys = items.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject value = items.getJSONObject(key);
            this.itemMap.put(key, value);
        }
    }

    public void setSystemStartTime(long startTime) {
        this.systemStartTime = startTime;
        this.systemDuration = this.getTimestamp() - startTime;
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

    public void appendItem(String name, JSONObject value) {
        if (null == this.itemMap) {
            this.itemMap = new HashMap<>();
        }
        this.itemMap.put(name, value);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();

        json.put("systemStartTime", this.systemStartTime);
        json.put("systemDuration", this.systemDuration);

        json.put("benchmark", this.benchmark.toJSON());

        JSONArray array = new JSONArray();
        Iterator<ConnectionReport> iter = this.serverConnMap.values().iterator();
        while (iter.hasNext()) {
            array.put(iter.next().toJSON());
        }
        json.put("connNums", array);

        JSONObject itemJson = new JSONObject();
        if (null != this.itemMap) {
            Iterator<String> keyIter = this.itemMap.keySet().iterator();
            while (keyIter.hasNext()) {
                String key = keyIter.next();
                itemJson.put(key, this.itemMap.get(key));
            }
        }
        json.put("items", itemJson);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toJSON();

        json.put("systemStartTime", this.systemStartTime);
        json.put("systemDuration", this.systemDuration);

        // 仅输出平均值
        json.put("benchmark", this.benchmark.toCompactJSON());

        JSONArray array = new JSONArray();
        Iterator<ConnectionReport> iter = this.serverConnMap.values().iterator();
        while (iter.hasNext()) {
            array.put(iter.next().toJSON());
        }
        json.put("connNums", array);

        JSONObject itemJson = new JSONObject();
        if (null != this.itemMap) {
            Iterator<String> keyIter = this.itemMap.keySet().iterator();
            while (keyIter.hasNext()) {
                String key = keyIter.next();
                itemJson.put(key, this.itemMap.get(key));
            }
        }
        json.put("items", itemJson);

        return json;
    }

    public JSONObject toDetailJSON() {
        JSONObject json = super.toJSON();

        json.put("systemStartTime", this.systemStartTime);
        json.put("systemDuration", this.systemDuration);

        // 仅输出平均值
        json.put("benchmark", this.benchmark.toDetailJSON());

        JSONArray array = new JSONArray();
        Iterator<ConnectionReport> iter = this.serverConnMap.values().iterator();
        while (iter.hasNext()) {
            array.put(iter.next().toJSON());
        }
        json.put("connNums", array);

        JSONObject itemJson = new JSONObject();
        if (null != this.itemMap) {
            Iterator<String> keyIter = this.itemMap.keySet().iterator();
            while (keyIter.hasNext()) {
                String key = keyIter.next();
                itemJson.put(key, this.itemMap.get(key));
            }
        }
        json.put("items", itemJson);

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
