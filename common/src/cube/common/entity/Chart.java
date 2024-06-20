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
import cube.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Chart 封装。
 */
public class Chart implements JSONable {

    public final static String AXIS_TYPE_VALUE = "value";
    public final static String AXIS_TYPE_CATEGORY = "category";

    public String name;

    public String desc;

    public long timestamp;

    public JSONObject title;

    public Legend legend;

    public List<Axis> xAxis;

    public List<Axis> yAxis;

    public List<Series> seriesList;

    public Chart(String name, String desc, long timestamp) {
        this.name = name;
        this.desc = desc;
        this.timestamp = timestamp;
        this.xAxis = new ArrayList<>();
        this.yAxis = new ArrayList<>();
        this.seriesList = new ArrayList<>();
    }

    public Chart(JSONObject json) {
        this.name = json.getString("name");
        this.desc = json.getString("desc");
        this.timestamp = json.has("timestamp") ? json.getLong("timestamp") : System.currentTimeMillis();

        if (json.has("title")) {
            this.title = json.getJSONObject("title");
        }

        if (json.has("legend")) {
            this.legend = new Legend(json.getJSONObject("legend"));
        }

        this.xAxis = new ArrayList<>();
        if (json.has("xAxis")) {
            JSONArray array = json.getJSONArray("xAxis");
            for (int i = 0; i < array.length(); ++i) {
                this.xAxis.add(new Axis(array.getJSONObject(i)));
            }
        }

        this.yAxis = new ArrayList<>();
        if (json.has("yAxis")) {
            JSONArray array = json.getJSONArray("yAxis");
            for (int i = 0; i < array.length(); ++i) {
                this.yAxis.add(new Axis(array.getJSONObject(i)));
            }
        }

        this.seriesList = new ArrayList<>();
        if (json.has("series")) {
            JSONArray array = json.getJSONArray("series");
            for (int i = 0; i < array.length(); ++i) {
                this.seriesList.add(new Series(array.getJSONObject(i)));
            }
        }
    }

    public Legend setLegend(List<String> data) {
        this.legend = new Legend(data);
        return this.legend;
    }

    public Axis setXAxis(String type) {
        this.xAxis.clear();
        this.xAxis.add(new Axis(type));
        return this.xAxis.get(0);
    }

    public Axis getXAxis() {
        return this.xAxis.isEmpty() ? null : this.xAxis.get(0);
    }

    public Axis setYAxis(String type) {
        this.yAxis.clear();
        this.yAxis.add(new Axis(type));
        return this.yAxis.get(0);
    }

    public Axis getYAxis() {
        return this.yAxis.isEmpty() ? null : this.yAxis.get(0);
    }

    public Series addSeries(String name, String type) {
        Series series = new Series(name, type);
        this.seriesList.add(series);
        return series;
    }

    public Series removeSeries(String name) {
        for (Series series : this.seriesList) {
            if (series.name.equals(name)) {
                this.seriesList.remove(series);
                return series;
            }
        }

        return null;
    }

    /*
    private void parseLegend() {
        if (this.chart.seriesList.size() <= 1) {
            return;
        }

        List<String> names = new ArrayList<>();

        for (Chart.Series series : this.chart.seriesList) {
            if (null == series.name) {
                return;
            }

            names.add(series.name);
        }

        this.legend = new Legend(names);
    }

    public void setXAxis(List<String> axis) {
        this.xAxis.addAll(axis);
    }

    public void setXAxis(JSONArray array) {
        for (int i = 0; i < array.length(); ++i) {
            this.xAxis.add(array.getString(i));
        }
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


    public boolean mergeSeries(Chart chart) {
        if (this.xAxis.size() != chart.xAxis.size()) {
            return false;
        }

        this.seriesList.addAll(chart.seriesList);
        return true;
    }

    public void setTimeline(List<Atom> atoms) {
        this.timeline = new Timeline(atoms);
    }*/

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("desc", this.desc);
        json.put("timestamp", this.timestamp);

        if (null != this.title) {
            json.put("title", this.title);
        }

        if (null != this.legend) {
            json.put("legend", this.legend.toJSON());
        }

        JSONArray xAxisArray = new JSONArray();
        for (Axis axis : this.xAxis) {
            xAxisArray.put(axis.toJSON());
        }
        json.put("xAxis", xAxisArray);

        JSONArray yAxisArray = new JSONArray();
        for (Axis axis : this.yAxis) {
            yAxisArray.put(axis.toJSON());
        }
        json.put("yAxis", yAxisArray);

        JSONArray seriesArray = new JSONArray();
        for (Series series : this.seriesList) {
            seriesArray.put(series.toJSON());
        }
        json.put("series", seriesArray);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    /**
     * 图例。
     */
    public class Legend {

        public String orient = "horizontal";

        public JSONArray data;

        public Legend(List<String> list) {
            this.data = new JSONArray();
            for (String name : list) {
                this.data.put(name);
            }
        }

        public Legend(JSONObject json) {
            this.orient = json.getString("orient");
            this.data = json.getJSONArray("data");
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("orient", this.orient);
            json.put("bottom", 10);
            json.put("data", this.data);
            return json;
        }
    }

    /**
     * 轴数据。
     */
    public class Axis {

        public String type;

        public JSONObject axisTick;

        public List<String> data;

        public Axis(String type) {
            this.type = type;
        }

        public Axis(JSONObject json) {
            this.type = json.getString("type");
            if (json.has("axisTick")) {
                this.axisTick = json.getJSONObject("axisTick");
            }
            if (json.has("data")) {
                this.data = JSONUtils.toStringList(json.getJSONArray("data"));
            }
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("type", this.type);
            if (null != this.axisTick) {
                json.put("axisTick", this.axisTick);
            }
            if (null != this.data) {
                json.put("data", JSONUtils.toStringArray(this.data));
            }
            return json;
        }
    }


    /**
     * 数据序列描述。
     */
    public class Series {

        // 图例名称
        public String name;

        public String type;

        public String stack;

        public JSONObject emphasis;

        public Label label;

        public List<Integer> data;

        public List<String> names;

        public Series(String name, String type) {
            this.name = name;
            this.type = type;
            this.data = new ArrayList<>();
        }

        public Series(JSONObject json) {
            this.type = json.getString("type");
            this.data = new ArrayList<>();

            JSONArray dataArray = json.getJSONArray("data");
            this.parseDataArray(dataArray);

            if (json.has("name")) {
                this.name = json.getString("name");
            }

            if (json.has("stack")) {
                this.stack = json.getString("stack");
            }

            if (json.has("emphasis")) {
                this.emphasis = json.getJSONObject("emphasis");
            }

            if (json.has("label")) {
                this.label = new Label(json.getJSONObject("label"));
            }
        }

        public Label setLabel(boolean show) {
            this.label = new Label();
            this.label.show = show;
            return this.label;
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
                    this.data.add(dataArray.getInt(i));
                }
            }
        }

        public void putData(String name, int value) {
            if (null == this.names) {
                this.names = new ArrayList<>();
            }

            this.names.add(name);
            this.data.add(value);
        }

        public int getData(int index) {
            return this.data.get(index);
        }

        public JSONArray toArray() {
            if (null != this.names && this.names.size() == this.data.size()) {
                return this.toNameValuePairArray();
            }
            else {
                return this.toValueArray();
            }
        }

        private JSONArray toValueArray() {
            JSONArray array = new JSONArray();
            for (Integer value : this.data) {
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
                int value = this.data.get(i);

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
            if (null != this.names && this.names.size() == this.data.size()) {
                json.put("data", this.toNameValuePairArray());
            }
            else {
                json.put("data", this.toValueArray());
            }

            if (null != this.name) {
                json.put("name", this.name);
            }

            if (null != this.stack) {
                json.put("stack", this.stack);
            }

            if (null != this.emphasis) {
                json.put("emphasis", this.emphasis);
            }

            if (null != this.label) {
                json.put("label", this.label.toJSON());
            }

            return json;
        }
    }

    /**
     * 标签。
     */
    public class Label {

        public boolean show;

        public String position;

        public Label() {
            this.show = true;
            this.position = "inside";
        }

        public Label(JSONObject json) {
            this.show = json.getBoolean("show");
            if (json.has("position")) {
                this.position = json.getString("position");
            }
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("show", this.show);
            json.put("position", this.position);
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
