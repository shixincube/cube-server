/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.ferry;

import cell.core.talk.TalkContext;
import cube.common.Domain;
import org.json.JSONObject;

/**
 * 用于标记 Ferry 的描述。
 */
public class Ticket {

    public final Domain domain;

    public final JSONObject licence;

    public final TalkContext talkContext;

    public final long sessionId;

    public Ticket(String domainName, JSONObject licence, TalkContext talkContext) {
        this.domain = new Domain(domainName);
        this.licence = licence;
        this.talkContext = talkContext;
        this.sessionId = (null != talkContext) ? talkContext.getSessionId() : 0;
    }

    public long getLicenceBeginning() {
        return this.licence.getLong("beginning");
    }

    public long getLicenceDuration() {
        return this.licence.getLong("duration");
    }

    public int getLicenceLimit() {
        return this.licence.getInt("limit");
    }
}
