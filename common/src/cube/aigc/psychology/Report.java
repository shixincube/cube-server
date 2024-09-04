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

package cube.aigc.psychology;

import cell.util.Utils;
import cube.common.JSONable;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 报告基类。
 */
public abstract class Report implements JSONable {

    private static final SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyyMMddHH");

    public final long sn;

    public final long contactId;

    public final long timestamp;

    protected String name;

    protected Attribute attribute;

    protected String summary;

    public Report(long contactId, Attribute attribute) {
        this.sn = Utils.generateSerialNumber();
        this.contactId = contactId;
        this.timestamp = System.currentTimeMillis();
        this.name = "AXL-" + sDateFormat.format(new Date(this.timestamp)) +
                String.format("%04d", Utils.randomInt(1, 9999));
        this.attribute = attribute;
    }

    public Report(long sn, long contactId, long timestamp, Attribute attribute) {
        this.sn = sn;
        this.contactId = contactId;
        this.timestamp = timestamp;
        this.name = "AXL-" + sDateFormat.format(new Date(this.timestamp)) +
                String.format("%04d", Utils.randomInt(1, 9999));
        this.attribute = attribute;
    }

    public Report(long sn, long contactId, long timestamp, String name, Attribute attribute) {
        this.sn = sn;
        this.contactId = contactId;
        this.timestamp = timestamp;
        this.name = name;
        this.attribute = attribute;
    }

    public Report(JSONObject json) {
        this.sn = json.getLong("sn");
        this.contactId = json.getLong("contactId");
        this.timestamp = json.getLong("timestamp");
        this.name = json.getString("name");
        this.attribute = new Attribute(json.getJSONObject("attribute"));
        if (json.has("summary")) {
            this.summary = json.getString("summary");
        }
    }

    public String getName() {
        return this.name;
    }

    public Attribute getAttribute() {
        return this.attribute;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSummary() {
        return this.summary;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("contactId", this.contactId);
        json.put("timestamp", this.timestamp);
        json.put("name", this.name);
        json.put("attribute", this.attribute.toJSON());
        if (null != this.summary) {
            json.put("summary", this.summary);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("contactId", this.contactId);
        json.put("timestamp", this.timestamp);
        json.put("name", this.name);
        json.put("attribute", this.attribute.toJSON());
        if (null != this.summary) {
            json.put("summary", this.summary);
        }
        return json;
    }
}
