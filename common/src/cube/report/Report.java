/*
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

import cube.common.JSONable;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 报告描述类。
 */
public class Report implements JSONable {

    /**
     * 报告名称。
     */
    private String name;

    /**
     * 报告时间戳。
     */
    private long timestamp;

    /**
     * 报告人描述。
     */
    private String reporter;

    public Report(String name) {
        this(name, System.currentTimeMillis());
    }

    public Report(String name, long timestamp) {
        this.name = name;
        this.timestamp = timestamp;
    }

    public Report(JSONObject json) {
        try {
            this.name = json.getString("name");
            this.timestamp = json.getLong("timestamp");
            this.reporter = json.getString("reporter");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return this.name;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public void setReporter(String reporter) {
        this.reporter = reporter;
    }

    public String getReporter() {
        return this.reporter;
    }

    protected void reset() {
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("timestamp", this.timestamp);
        json.put("reporter", this.reporter);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    public static String extractName(JSONObject json) throws JSONException {
        return json.getString("name");
    }
}
