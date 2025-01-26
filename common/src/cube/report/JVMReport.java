/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.report;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * JVM 信息报告。
 */
public class JVMReport extends Report {

    public final static String NAME = "JVMReport";

    /** JVM 最大可用内存。 */
    public long maxMemory;

    /** JVM 已使用的系统内存。 */
    public long totalMemory;

    /** JVM 空闲的系统内存。 */
    public long freeMemory;

    /** 内核启动时间。 */
    public long systemStartTime;

    /** 截止本次快照生成时内核运行的时长，单位：毫秒。 */
    public long systemDuration;

    public JVMReport(String reporter, long timestamp) {
        super(JVMReport.NAME, timestamp);
        this.setReporter(reporter);

        this.maxMemory = Runtime.getRuntime().maxMemory();
        this.totalMemory = Runtime.getRuntime().totalMemory();
        this.freeMemory = Runtime.getRuntime().freeMemory();
    }

    public JVMReport(JSONObject json) {
        super(json);

        try {
            this.maxMemory = json.getLong("maxMemory");
            this.totalMemory = json.getLong("totalMemory");
            this.freeMemory = json.getLong("freeMemory");
            this.systemStartTime = json.getLong("systemStartTime");
            this.systemDuration = json.getLong("systemDuration");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void dump() {
        this.maxMemory = 0;
        this.totalMemory = 0;
        this.freeMemory = 0;
    }

    public void setSystemStartTime(long startTime) {
        this.systemStartTime = startTime;
        this.systemDuration = this.getTimestamp() - startTime;
    }

    public void scaleValue(double factor) {
        this.maxMemory = Math.round((double) this.maxMemory / factor);
        this.totalMemory = Math.round((double) this.totalMemory / factor);
        this.freeMemory = Math.round((double) this.freeMemory / factor);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("maxMemory", this.maxMemory);
            json.put("totalMemory", this.totalMemory);
            json.put("freeMemory", this.freeMemory);
            json.put("systemStartTime", this.systemStartTime);
            json.put("systemDuration", this.systemDuration);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}
