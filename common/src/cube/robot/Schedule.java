/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.robot;

import cube.common.JSONable;
import org.json.JSONObject;

public class Schedule implements JSONable {

    public final static int STATE_NORMAL = 0;

    public final static int STATE_SUSPEND = 1;

    public final static int STATE_INVALID = 2;

    public long sn;

    public long taskId;

    public long accountId;

    public long publishTime;

    public long releaseTime;

    public long pushTime = 0;

    public int state = STATE_NORMAL;

    public Task task;

    public Schedule(long sn, long taskId, long accountId, long publishTime, long releaseTime, long pushTime, int state) {
        this.sn = sn;
        this.taskId = taskId;
        this.accountId = accountId;
        this.publishTime = publishTime;
        this.releaseTime = releaseTime;
        this.pushTime = pushTime;
        this.state = state;
    }

    public Schedule(JSONObject json) {
        this.sn = json.getLong("sn");
        this.taskId = json.getLong("taskId");
        this.accountId = json.getLong("accountId");
        this.publishTime = json.getLong("publishTime");
        this.releaseTime = json.getLong("releaseTime");
        this.pushTime = json.getLong("pushTime");
        this.state = json.getInt("state");
        if (json.has("task")) {
            this.task = new Task(json.getJSONObject("task"));
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("taskId", this.taskId);
        json.put("accountId", this.accountId);
        json.put("publishTime", this.publishTime);
        json.put("releaseTime", this.releaseTime);
        json.put("pushTime", this.pushTime);
        json.put("state", this.state);
        if (null != this.task) {
            json.put("task", this.task.toJSON());
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return toJSON();
    }
}
