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

package cube.common.entity;

import cube.aigc.atom.Atom;
import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Chart 序列封装。
 */
public class ChartSeries implements JSONable {

    public String name;

    public String desc;

    public long timestamp;

    public List<String> xAxis;

    public List<String> xAxisDesc;

    public List<Series> seriesList;

    public Timeline timeline;

    public String label;

    public JSONObject option;

    public ChartSeries(String name, String desc, long timestamp) {
        this.name = name;
        this.desc = desc;
        this.timestamp = timestamp;
        this.xAxis = new ArrayList<>();
        this.xAxisDesc = new ArrayList<>();
        this.seriesList = new ArrayList<>();
    }

    public ChartSeries(JSONObject json) {
        this.name = json.getString("name");
        this.desc = json.getString("desc");
        this.timestamp = json.has("timestamp") ? json.getLong("timestamp") : System.currentTimeMillis();
        this.xAxis = new ArrayList<>();
        this.xAxisDesc = new ArrayList<>();
        this.seriesList = new ArrayList<>();

        this.setXAxis(json.getJSONArray("xAxis"));

        if (json.has("type") && json.has("data")) {
            Series series = new Series(json.getString("type"), json.getJSONArray("data"));
            this.seriesList.add(series);
        }
        else if (json.has("series")) {
            JSONArray seriesArray = json.getJSONArray("series");
            for (int i = 0; i < seriesArray.length(); ++i) {
                Series series = new Series(seriesArray.getJSONObject(i));
                this.seriesList.add(series);
            }
        }

        if (json.has("option")) {
            this.option = json.getJSONObject("option");
        }
    }

    public void setXAxis(List<String> axis) {
        this.xAxis.addAll(axis);
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

    public void setXAxisDesc(List<String> descList) {
        this.xAxisDesc.addAll(descList);
    }

    public void setData(String type, JSONArray data) {
        Series series = new Series(type, data);
        this.seriesList.add(series);
    }

    public void setData(String type, JSONArray data, String legend) {
        Series series = new Series(type, data);
        series.name = legend;
        this.seriesList.add(series);
    }

    public Series getSeries() {
        return this.seriesList.get(0);
    }

    /**
     * 合并序列。
     *
     * @param chartSeries
     * @return
     */
    public boolean mergeSeries(ChartSeries chartSeries) {
        if (this.xAxis.size() != chartSeries.xAxis.size()) {
            return false;
        }

        this.seriesList.addAll(chartSeries.seriesList);
        return true;
    }

    public void setTimeline(List<Atom> atoms) {
        this.timeline = new Timeline(atoms);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("desc", this.desc);
        json.put("timestamp", this.timestamp);
        json.put("xAxis", this.getXAxis());

        if (this.seriesList.size() == 1) {
            json.put("type", this.seriesList.get(0).type);
            json.put("data", this.seriesList.get(0).toArray());
        }
        else {
            JSONArray array = new JSONArray();
            for (Series series : this.seriesList) {
                array.put(series.toJSON());
            }
            json.put("series", array);
        }

        if (null != this.option) {
            json.put("option", this.option);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = this.toJSON();
        json.remove("name");
        return json;
    }


    /**
     * 数据序列描述。
     */
    public class Series {

        // 图例名称
        public String name;

        public String type;

        public List<Integer> values;

        public List<String> names;

        public Series(String type, JSONArray data) {
            this.type = type;
            this.values = new ArrayList<>();
            this.parseDataArray(data);
        }

        public Series(JSONObject json) {
            this.type = json.getString("type");
            this.values = new ArrayList<>();

            JSONArray dataArray = json.getJSONArray("data");
            this.parseDataArray(dataArray);

            if (json.has("name")) {
                this.name = json.getString("name");
            }
        }

        private void parseDataArray(JSONArray dataArray) {
            if (dataArray.get(0) instanceof JSONObject) {
                // Data pair
                for (int i = 0; i < dataArray.length(); ++i) {
                    JSONObject pair = dataArray.getJSONObject(i);
                    String name = pair.getString("name");
                    int value = pair.getInt("value");

                    this.putData(name, value);
                }
            }
            else {
                for (int i = 0; i < dataArray.length(); ++i) {
                    this.values.add(dataArray.getInt(i));
                }
            }
        }

        public void putData(String name, int value) {
            if (null == this.names) {
                this.names = new ArrayList<>();
            }

            this.names.add(name);
            this.values.add(value);
        }

        public int getValue(int index) {
            return this.values.get(index);
        }

        public JSONArray toArray() {
            if (null != this.names && this.names.size() == this.values.size()) {
                return this.toNameValuePairArray();
            }
            else {
                return this.toValueArray();
            }
        }

        private JSONArray toValueArray() {
            JSONArray array = new JSONArray();
            for (Integer value : this.values) {
                array.put(value.intValue());
            }
            return array;
        }

        private JSONArray toNameValuePairArray() {
            if (null == this.names) {
                return null;
            }

            JSONArray array = new JSONArray();
            for (int i = 0; i < this.names.size(); ++i) {
                String name = this.names.get(i);
                int value = this.values.get(i);

                JSONObject data = new JSONObject();
                data.put("name", name);
                data.put("value", value);

                array.put(data);
            }
            return array;
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("type", this.type);

            // 如果 names 和 data 长度相同，则将数据形式设置为 name/value 形式
            if (null != this.names && this.names.size() == this.values.size()) {
                json.put("data", this.toNameValuePairArray());
            }
            else {
                json.put("data", this.toValueArray());
            }

            if (null != this.name) {
                json.put("name", this.name);
            }

            return json;
        }
    }

    /**
     * 序列时间线描述。
     */
    public class Timeline {

        public List<TimePoint> points;

        public Timeline(List<Atom> atoms) {
            this.points = new ArrayList<>(atoms.size());
            for (Atom atom : atoms) {
                TimePoint point = new TimePoint(atom.getYear(), atom.getMonth(), atom.getDate());
                this.points.add(point);
            }
        }

        public TimePoint first() {
            return this.points.get(0);
        }

        public TimePoint last() {
            return this.points.get(this.points.size() - 1);
        }
    }


    public class TimePoint {
        public final int year;
        public final int month;
        public final int date;

        public TimePoint(int year, int month, int date) {
            this.year = year;
            this.month = month;
            this.date = date;
        }
    }
}
