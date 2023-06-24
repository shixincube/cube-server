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

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class ChartReaction implements JSONable {

    public long sn;

    public final String[] primary;

    public String secondary = null;

    public String tertiary = null;

    public String quaternary = null;

    public final String seriesName;

    public final long timestamp;

    public ChartReaction(String[] primary, String seriesName, long timestamp) {
        this.sn = 0;
        this.primary = primary;
        this.seriesName = seriesName;
        this.timestamp = timestamp;
    }

    public ChartReaction(JSONObject json) {
        this.sn = json.has("sn") ? json.getLong("sn") : 0;

        JSONArray array = json.getJSONArray("primary");
        this.primary = new String[array.length()];
        for (int i = 0; i < array.length(); ++i) {
            this.primary[i] = array.getString(i);
        }

        this.seriesName = json.getString("seriesName");
        if (json.has("timestamp")) {
            this.timestamp = json.getLong("timestamp");
        }
        else {
            this.timestamp = System.currentTimeMillis();
        }
    }

    public String serializePrimary() {
        StringBuilder buf = new StringBuilder();
        for (String word : this.primary) {
            buf.append(word);
            buf.append(",");
        }
        buf.deleteCharAt(buf.length() - 1);
        return buf.toString();
    }

    public int matchWordNum(List<String> words) {
        int num = 0;
        for (String word : words) {
            if (null != this.secondary && this.secondary.contains(word)) {
                ++num;
            }
            else if (null != this.tertiary && this.tertiary.contains(word)) {
                ++num;
            }
            else if (null != this.quaternary && this.quaternary.contains(word)) {
                ++num;
            }
        }
        return num;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ChartReaction) {
            ChartReaction other = (ChartReaction) obj;
            return other.sn == this.sn && other.timestamp == this.timestamp;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return (int)(this.sn + this.primary[0].hashCode() * 7 + this.timestamp);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);

        JSONArray array = new JSONArray();
        for (String word : this.primary) {
            array.put(word);
        }
        json.put("primary", array);

        if (null != this.secondary) {
            json.put("secondary", this.secondary);
        }
        if (null != this.tertiary) {
            json.put("tertiary", this.tertiary);
        }
        if (null != this.quaternary) {
            json.put("quaternary", this.quaternary);
        }
        json.put("seriesName", this.seriesName);
        json.put("timestamp", this.timestamp);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
