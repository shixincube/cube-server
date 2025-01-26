/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
