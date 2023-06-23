/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChartSeries implements JSONable {

    public String name;

    public String desc;

    public long timestamp;

    public String type;

    public List<String> xAxis;

    public List<Integer> data;

    public JSONObject option;

    public ChartSeries(String name, String desc, long timestamp, String type) {
        this.name = name;
        this.desc = desc;
        this.timestamp = timestamp;
        this.type = type;
        this.xAxis = new ArrayList<>();
        this.data = new ArrayList<>();
    }

    public ChartSeries(JSONObject json) {
        this.name = json.getString("name");
        this.desc = json.getString("desc");
        this.timestamp = json.has("timestamp") ? json.getLong("timestamp") : System.currentTimeMillis();
        this.type = json.getString("type");

        this.xAxis = new ArrayList<>();
        this.data = new ArrayList<>();

        this.setXAxis(json.getJSONArray("xAxis"));
        this.setData(json.getJSONArray("data"));
        if (json.has("option")) {
            this.option = json.getJSONObject("option");
        }
    }

    public void setXAxis(JSONArray array) {
        for (int i = 0; i < array.length(); ++i) {
            this.xAxis.add(array.getString(i));
        }
    }

    public JSONArray getXAxis() {
        JSONArray result = new JSONArray();
        if (null != this.xAxis) {
            for (String value : this.xAxis) {
                result.put(value);
            }
        }
        return result;
    }

    public void setData(JSONArray array) {
        for (int i = 0; i < array.length(); ++i) {
            this.data.add(array.getInt(i));
        }
    }

    public JSONArray getData() {
        JSONArray result = new JSONArray();
        if (null != this.data) {
            for (Integer value : this.data) {
                result.put(value.intValue());
            }
        }
        return result;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("desc", this.desc);
        json.put("timestamp", this.timestamp);
        json.put("type", this.type);
        json.put("xAxis", this.getXAxis());
        json.put("data", this.getData());
        if (null != this.option) {
            json.put("option", this.option);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
