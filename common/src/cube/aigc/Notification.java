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

package cube.aigc;

import cell.util.Utils;
import cube.common.JSONable;
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

    public Notification(String title, String content) {
        this.id = Utils.generateSerialNumber();
        this.type = TYPE_NORMAL;
        this.state = STATE_ENABLED;
        this.title = title;
        this.content = content;
        this.date = Utils.gsDateFormat.format(new Date(System.currentTimeMillis()));
    }

    public Notification(long id, String type, int state, String title, String content, String date) {
        this.id = id;
        this.type = type;
        this.state = state;
        this.title = title;
        this.content = content;
        this.date = date;
    }

    public Notification(JSONObject json) {
        this.id = json.getLong("id");
        this.type = json.getString("type");
        this.state = json.getInt("state");
        this.title = json.getString("title");
        this.content = json.getString("content");
        this.date = json.getString("date");
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
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
