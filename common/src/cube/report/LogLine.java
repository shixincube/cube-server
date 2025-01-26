/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.report;

import cell.util.log.LogLevel;
import cube.common.JSONable;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 日志行。
 */
public class LogLine implements JSONable {

    private final static SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    public int level;

    public String tag;

    public String text;

    public long time;

    public LogLine(int level, String tag, String text, long time) {
        this.level = level;
        this.tag = tag;
        this.text = text;
        this.time = time;
    }

    public LogLine(JSONObject json) {
        try {
            this.level = json.getInt("level");
            this.tag = json.getString("tag");
            this.text = json.getString("text");
            this.time = json.getLong("time");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(TIME_FORMAT.format(new Date(this.time)));
        buf.append(" [");
        buf.append(LogLevel.parse(this.level).name());
        buf.append("] ");
        buf.append(this.tag);
        buf.append(" ");
        buf.append(this.text);
        return buf.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("level", this.level);
            json.put("tag", this.tag);
            json.put("text", this.text);
            json.put("time", this.time);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
