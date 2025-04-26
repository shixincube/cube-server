/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.app;

import cell.util.Utils;
import cube.common.JSONable;
import cube.util.TimeUtils;
import org.json.JSONObject;

import java.util.Date;

public class Notification implements JSONable {

    public final static String TYPE_NORMAL = "normal";

    public final static String TYPE_POPUP = "popup";

    public final static int STATE_ENABLED = 1;

    public final static int STATE_DISABLED = 0;

    public long id;

    public String type;

    public int state;

    public String title;

    public String content;

    public String date;

    public long timestamp;

    public Notification(String title, String content) {
        this.id = Utils.generateSerialNumber();
        this.type = TYPE_NORMAL;
        this.state = STATE_ENABLED;
        this.title = title;
        this.content = content;
        this.date = Utils.gsDateFormat.format(new Date(System.currentTimeMillis()));
        this.timestamp = System.currentTimeMillis();
    }

    public Notification(long id, String type, int state, String title, String content, String date) {
        this.id = id;
        this.type = type;
        this.state = state;
        this.title = title;
        this.content = content;
        this.date = date;
        this.timestamp = TimeUtils.unformatDate(date);
    }

    public Notification(JSONObject json) {
        this.id = json.getLong("id");
        this.type = json.getString("type");
        this.state = json.getInt("state");
        this.title = json.getString("title");
        this.content = json.getString("content");
        this.date = json.getString("date");
        this.timestamp = json.getLong("timestamp");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("type", this.type);
        json.put("state", this.state);
        json.put("title", this.title);
        json.put("content", this.content);
        json.put("date", this.date);
        json.put("timestamp", this.timestamp);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
