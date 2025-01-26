/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc;

import cell.util.Utils;
import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 提示词记录。
 */
public class PromptRecord implements JSONable {

    public final long id;

    public final String act;

    public final String prompt;

    public final boolean readonly;

    public PromptRecord(long id, String act, String prompt, boolean readonly) {
        this.id = id;
        this.act = act;
        this.prompt = prompt;
        this.readonly = readonly;
    }

    public PromptRecord(JSONObject json) {
        this.id = json.has("id") ? json.getLong("id") : Utils.generateSerialNumber();
        this.act = json.getString("act");
        this.prompt = json.getString("prompt");
        this.readonly = json.getBoolean("readonly");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("act", this.act);
        json.put("prompt", this.prompt);
        json.put("readonly", this.readonly);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
