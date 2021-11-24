/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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
import org.json.JSONObject;

/**
 * 联系人分区参与人。
 */
public class ContactZoneParticipant implements JSONable {

    public final Long id;

    public final ContactZoneParticipantType type;

    public final long timestamp;

    public final String postscript;

    public ContactZoneParticipantState state;

    public ContactZoneParticipant(JSONObject json) {
        this(json.getLong("id"), ContactZoneParticipantType.parse(json.getInt("type")),
                json.getLong("timestamp"),
                json.has("postscript") ? json.getString("postscript") : null);
    }

    public ContactZoneParticipant(Long id, ContactZoneParticipantType type, long timestamp, String postscript) {
        this(id, type, timestamp, postscript, ContactZoneParticipantState.Normal);
    }

    public ContactZoneParticipant(Long id, ContactZoneParticipantType type, long timestamp, String postscript, ContactZoneParticipantState state) {
        this.id = id;
        this.type = type;
        this.timestamp = timestamp;
        this.postscript = (null != postscript) ? postscript : "";
        this.state = state;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (null != object && object instanceof ContactZoneParticipant) {
            ContactZoneParticipant other = (ContactZoneParticipant) object;
            if (other.id.equals(this.id) && other.type.code == this.type.code) {
                return true;
            }
        }

        return false;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id.longValue());
        json.put("type", this.type.code);
        json.put("timestamp", this.timestamp);
        json.put("state", this.state.code);
        json.put("postscript", this.postscript);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
