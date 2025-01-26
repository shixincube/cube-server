/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.attachment.ui;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 事件结果。
 */
public class EventResult implements JSONable {

    private Event event;

    public EventResult(Event event) {
        this.event = event;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = this.event.toCompactJSON();
        if (null != this.event.resultResource) {
            json.put("result", this.event.resultResource.toJSON());
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
