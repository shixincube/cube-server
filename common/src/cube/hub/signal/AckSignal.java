/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.signal;

import org.json.JSONObject;

/**
 * 应答信令。
 */
public class AckSignal extends Signal {

    public final static String NAME = "Ack";

    public AckSignal() {
        super(NAME);
    }

    public AckSignal(JSONObject json) {
        super(json);
    }
}
