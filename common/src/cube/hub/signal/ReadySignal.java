/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.signal;

import cell.core.talk.TalkContext;
import cube.common.entity.ClientDescription;
import org.json.JSONObject;

/**
 * 就绪信令。
 */
public class ReadySignal extends Signal {

    public final static String NAME = "Ready";

    public TalkContext talkContext;

    public ReadySignal(ClientDescription description) {
        super(NAME, description);
    }

    public ReadySignal(JSONObject json) {
        super(json);
    }
}
