/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.event;

import org.json.JSONObject;

/**
 * 告警事件。
 */
public class AlarmEvent extends WeChatEvent {

    public final static String NAME = "Alarm";

    private String alarmName;

    private JSONObject alarmData;

    public AlarmEvent(String alarmName, JSONObject alarmData) {
        super(NAME);
        this.alarmName = alarmName;
        this.alarmData = alarmData;
    }

    public AlarmEvent(JSONObject json) {
        super(json);
        this.alarmName = json.getString("alarmName");
        this.alarmData = json.getJSONObject("alarmData");
    }

    public String getAlarmName() {
        return this.alarmName;
    }

    public JSONObject getAlarmData() {
        return this.alarmData;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("alarmName", this.alarmName);
        json.put("alarmData", this.alarmData);
        return json;
    }
}
