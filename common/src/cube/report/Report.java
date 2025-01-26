/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
