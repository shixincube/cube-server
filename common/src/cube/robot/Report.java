/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

/**
 * Roboengine 的报告描述。
 */
public class Report implements JSONable {

    public long id = 0;

    public Account account;

    public Device device;

    public String source;

    public String category;

    public String title;

    public JSONObject content;

    public long creationTime;

    private long receivedTime;

    private String address;

    public Report(long id) {
        this.id = id;
    }

    public Report(JSONObject json) {
        this.account = new Account(json.getJSONObject("account"));
        this.device = new Device(json.getJSONObject("device"));
        this.source = json.getString("source");
        this.category = json.getString("category");
        this.title = json.getString("title");
        this.content = json.getJSONObject("content");
        this.creationTime = json.getLong("creationTime");

        if (json.has("receivedTime")) {
            this.receivedTime = json.getLong("receivedTime");
        }

        if (json.has("address")) {
            this.address = json.getString("address");
        }

        if (json.has("id")) {
            this.id = json.getLong("id");
        }
    }

    public void setReceivedTime(long time) {
        this.receivedTime = time;
    }

    public long getReceivedTime() {
        return this.receivedTime;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return this.address;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("account", this.account.toJSON());
        json.put("device", this.device.toJSON());
        json.put("source", this.source);
        json.put("category", this.category);
        json.put("title", this.title);
        json.put("content", this.content);
        json.put("creationTime", this.creationTime);
        json.put("receivedTime", this.receivedTime);
        if (null != this.address) {
            json.put("address", this.address);
        }
        if (this.id != 0) {
            json.put("id", this.id);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return toJSON();
    }
}
