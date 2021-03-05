/**
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

    public JVMReport(String reporter) {
        super(JVMReport.NAME);
        this.setReporter(reporter);

        this.maxMemory = Runtime.getRuntime().maxMemory();
        this.totalMemory = Runtime.getRuntime().totalMemory();
        this.freeMemory = Runtime.getRuntime().freeMemory();
    }

    public JVMReport(String reporter, long timestamp) {
        super(JVMReport.NAME, timestamp);
        this.setReporter(reporter);
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
