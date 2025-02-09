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

    public String number;

    private long id = 0;

    public ConversationRelation() {
        this.name = "Anonymous";
    }

    public ConversationRelation(String name, long reportSn, String number) {
        this.name = name;
        this.reportSn = reportSn;
        this.number = number;
    }

    public ConversationRelation(JSONObject json) {
        this.name = json.getString("name");
        if (json.has("reportSn")) {
            this.reportSn = json.getLong("reportSn");
        }
        if (json.has("number")) {
            this.number = json.getString("number");
        }
    }

    public synchronized long getId() {
        if (0 == this.id) {
            StringBuilder buf = new StringBuilder();
            buf.append(this.name);
            if (null != this.number) {
                buf.append(this.number);
            }
            long hash = Cryptology.getInstance().fastHash(buf.toString());
            this.id = Math.abs(hash);
        }
        return this.id;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        if (this.reportSn > 0) {
            json.put("reportSn", this.reportSn);
        }
        if (null != this.number) {
            json.put("number", this.number);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
