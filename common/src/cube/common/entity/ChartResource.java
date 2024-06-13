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

import org.json.JSONObject;

/**
 * 图表资源。
 */
public class ChartResource extends ComplexResource {

    private String title;

    private Chart chart;

    public ChartResource(String title, Chart chart) {
        super(Subject.Chart);
        this.title = title;
        this.chart = chart;
    }

    public ChartResource(JSONObject json) {
        super(Subject.Chart, json);

        JSONObject payload = json.getJSONObject("payload");
        this.title = payload.getString("title");
        this.chart = new Chart(payload.getJSONObject("chart"));
    }

    public String getTitle() {
        return this.title;
    }

    public Chart getChart() {
        return this.chart;
    }

//    public String makeDataPlainString() {
//        StringBuilder buf = new StringBuilder();
//        buf.append(this.title).append("。\n");
//        buf.append(this.chart.name).append("：\n");
//
//        for (Chart.Series series : this.chart.seriesList) {
//            String legend = series.name;
//
//            for (int i = 0; i < this.chart.xAxis.size(); ++i) {
//                String desc = this.chart.xAxisDesc.get(i);
//                int value = series.getValue(i);
//                buf.append(desc).append(legend).append("：").append(value)
//                    .append("。\n");
//            }
//
//            buf.append("\n");
//        }
//        return buf.toString();
//    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();

        JSONObject payload = new JSONObject();
        payload.put("title", this.title);
        payload.put("chart", this.chart.toJSON());

        json.put("payload", payload);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
