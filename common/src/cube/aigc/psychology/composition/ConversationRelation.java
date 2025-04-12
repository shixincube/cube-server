/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import cell.util.Cryptology;
import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 报告数据关系描述。
 */
public class ConversationRelation implements JSONable {

    public String name;

    public long reportSn = 0;

    private long uid = 0;

    public ConversationRelation() {
        this.name = "Anonymous";
    }

    public ConversationRelation(String name, long reportSn) {
        this.name = name;
        this.reportSn = reportSn;
    }

    public ConversationRelation(JSONObject json) {
        this.name = json.getString("name");
        if (json.has("reportSn")) {
            this.reportSn = json.getLong("reportSn");
        }
        if (json.has("uid")) {
            this.uid = json.getLong("uid");
        }
    }

    public synchronized long getId() {
        if (0 == this.uid) {
            long hash = Cryptology.getInstance().fastHash(this.name);
            this.uid = Math.abs(hash);
        }
        return this.uid;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        if (this.reportSn > 0) {
            json.put("reportSn", this.reportSn);
        }
        if (this.uid > 0) {
            json.put("uid", this.uid);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
