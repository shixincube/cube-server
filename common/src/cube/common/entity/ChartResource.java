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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 图表资源。
 */
public class ChartResource extends ComplexResource {

    public String title;

    public String titleAlign = "center";

    public Legend legend;

    public ChartSeries chartSeries;

    public ChartResource(String title, ChartSeries chartSeries) {
        super(Subject.Chart);
        this.title = title;
        this.chartSeries = chartSeries;
        this.parseLegend();
    }

    public ChartResource(JSONObject json) {
        super(Subject.Chart, json);

        JSONObject payload = json.getJSONObject("payload");
        this.title = payload.getString("title");
        this.titleAlign = payload.getString("titleAlign");
        this.chartSeries = new ChartSeries(payload.getJSONObject("series"));

        if (payload.has("legend")) {
            this.legend = new Legend(payload.getJSONObject("legend"));
        }
    }

    public String makeDataPlainString() {
        StringBuilder buf = new StringBuilder();
        buf.append(this.title).append("。\n");
        buf.append(this.chartSeries.name).append("：\n");

        for (ChartSeries.Series series : this.chartSeries.seriesList) {
            String legend = series.name;

            for (int i = 0; i < this.chartSeries.xAxis.size(); ++i) {
                String desc = this.chartSeries.xAxisDesc.get(i);
                int value = series.getValue(i);
                buf.append(desc).append(legend).append("是").append(value)
                    .append("。\n");
            }

            buf.append("\n");
        }
        return buf.toString();
    }

    private void parseLegend() {
        if (this.chartSeries.seriesList.size() <= 1) {
            return;
        }

        List<String> names = new ArrayList<>();

        for (ChartSeries.Series series : this.chartSeries.seriesList) {
            if (null == series.name) {
                return;
            }

            names.add(series.name);
        }

        this.legend = new Legend(names);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();

        JSONObject payload = new JSONObject();
        payload.put("title", this.title);
        payload.put("titleAlign", this.titleAlign);
        payload.put("series", this.chartSeries.toJSON());

        if (null != this.legend) {
            payload.put("legend", this.legend.toJSON());
        }

        json.put("payload", payload);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }


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
}
