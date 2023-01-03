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

package cube.hub.event;

import cube.common.entity.ContactZone;
import cube.hub.data.DataHelper;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 联系人 Zone 数据事件。
 */
public class ContactZoneEvent extends WeChatEvent {

    public final static String NAME = "ContactZone";

    private ContactZone contactZone;

    private int beginIndex;

    private int endIndex;

    private int totalSize;

    public ContactZoneEvent(ContactZone contactZone, int beginIndex, int endIndex, int totalSize) {
        super(NAME);
        this.contactZone = contactZone;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        this.totalSize = totalSize;
    }

    public ContactZoneEvent(JSONObject json) {
        super(json);
        this.contactZone = new ContactZone(json.getJSONObject("zone"));
        this.beginIndex = json.getInt("begin");
        this.endIndex = json.getInt("end");
        this.totalSize = json.getInt("total");
    }

    public ContactZone getContactZone() {
        return this.contactZone;
    }

    public int getBeginIndex() {
        return this.beginIndex;
    }

    public int getEndIndex() {
        return this.endIndex;
    }

    public int getTotalSize() {
        return this.totalSize;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("zone", this.contactZone.toJSON());
        json.put("begin", this.beginIndex);
        json.put("end", this.endIndex);
        json.put("total", this.totalSize);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        json.put("begin", this.beginIndex);
        json.put("end", this.endIndex);
        json.put("total", this.totalSize);

        // 过滤数据
        JSONObject zoneJson = new JSONObject(this.contactZone.toJSON().toMap());
        JSONArray participants = zoneJson.getJSONArray("participants");
        for (int i = 0; i < participants.length(); ++i) {
            JSONObject part = participants.getJSONObject(i);
            if (part.has("linkedContact")) {
                part.put("linkedContact",
                        DataHelper.filterContactAvatarFileLabel(part.getJSONObject("linkedContact")));
            }
        }

        json.put("zone", zoneJson);
        return json;
    }
}
