/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 报告数据关系描述。
 */
public class ReportRelation implements JSONable {

    public String name;

    public long reportSn;

    public String number;

    public ReportRelation(String name, long reportSn, String number) {
        this.name = name;
        this.reportSn = reportSn;
        this.number = number;
    }

    public ReportRelation(JSONObject json) {
        this.name = json.getString("name");
        this.reportSn = json.getLong("reportSn");
        if (json.has("number")) {
            this.number = json.getString("number");
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("reportSn", this.reportSn);
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
