/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm.event;

import cube.common.JSONable;
import cube.common.entity.CommField;
import cube.common.entity.CommFieldEndpoint;
import org.json.JSONObject;

/**
 * 场域的更新信息。
 */
public class CommFieldUpdate implements JSONable {

    protected CommField field;

    protected CommFieldEndpoint endpoint;

    public CommFieldUpdate(CommField field, CommFieldEndpoint endpoint) {
        this.field = field;
        this.endpoint = endpoint;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("field", this.field.toJSON());
        json.put("endpoint", this.endpoint.toCompactJSON());
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

}
