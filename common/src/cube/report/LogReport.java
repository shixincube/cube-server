/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
