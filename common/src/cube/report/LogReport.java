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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 日志报告。
 */
public class LogReport extends Report {

    public final static String NAME = "Log";

    private List<LogLine> logLines;

    public LogReport(String reporter) {
        super(LogReport.NAME);
        this.setReporter(reporter);
        this.logLines = new ArrayList<>();
    }

    public LogReport(JSONObject json) {
        super(json);
        try {
            JSONArray array = json.getJSONArray("lines");
            this.logLines = new ArrayList<>();
            for (int i = 0; i < array.length(); ++i) {
                JSONObject line = array.getJSONObject(i);
                this.logLines.add(new LogLine(line));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void addLogs(List<LogLine> logs) {
        this.logLines.addAll(logs);
    }

    public List<LogLine> getLogs() {
        return this.logLines;
    }

    public void clear() {
        this.logLines.clear();
        this.reset();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();

        JSONArray lines = new JSONArray();
        for (LogLine line : this.logLines) {
            lines.put(line.toJSON());
        }

        try {
            json.put("lines", lines);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }
}
