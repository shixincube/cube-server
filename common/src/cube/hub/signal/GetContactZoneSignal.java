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

package cube.hub.signal;

import cube.common.entity.ContactZoneParticipantType;
import org.json.JSONObject;

/**
 * 获取联系人分区信令。
 */
public class GetContactZoneSignal extends Signal {

    public final static String NAME = "GetContactZone";

    private ContactZoneParticipantType participantType;

    private int beginIndex;

    private int endIndex;

    public GetContactZoneSignal(String channelCode, ContactZoneParticipantType participantType,
                                int beginIndex, int endIndex) {
        super(NAME);
        setCode(channelCode);
        this.participantType = participantType;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
    }

    public GetContactZoneSignal(JSONObject json) {
        super(json);
        this.participantType = ContactZoneParticipantType.parse(json.getInt("participantType"));
        this.beginIndex = json.getInt("begin");
        this.endIndex = json.getInt("end");
    }

    public ContactZoneParticipantType getParticipantType() {
        return this.participantType;
    }

    public int getBeginIndex() {
        return this.beginIndex;
    }

    public int getEndIndex() {
        return this.endIndex;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("participantType", this.participantType.code);
        json.put("begin", this.beginIndex);
        json.put("end", this.endIndex);
        return json;
    }
}
