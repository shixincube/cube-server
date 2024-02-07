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
