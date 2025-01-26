/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.attachment;

import cell.util.Utils;
import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 内容附件。
 */
public abstract class Attachment implements JSONable {

    protected long id;

    protected String type;

    public Attachment(String type) {
        this.id = Utils.generateSerialNumber();
        this.type = type;
    }

    public Attachment(JSONObject json) {
        this.id = json.getLong("id");
        this.type = json.getString("type");
    }

    public long getId() {
        return this.id;
    }

    public String getType() {
        return this.type;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("type", this.type);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
