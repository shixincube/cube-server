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

    public final String title;

    public final String content;

    public final String act;

    public final boolean readonly;

    public PromptRecord(long id, String title, String content, String act, boolean readonly) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.act = act;
        this.readonly = readonly;
    }

    public PromptRecord(JSONObject json) {
        this.id = json.has("id") ? json.getLong("id") : Utils.generateSerialNumber();
        this.title = json.getString("title");
        this.content = json.getString("content");
        this.act = json.getString("act");
        this.readonly = json.getBoolean("readonly");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("title", this.title);
        json.put("content", this.content);
        json.put("act", this.act);
        json.put("readonly", this.readonly);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
