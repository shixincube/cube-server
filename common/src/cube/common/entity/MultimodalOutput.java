/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cell.util.Utils;
import cube.aigc.Usage;
import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 多模态输出。
 */
public class MultimodalOutput implements JSONable {

    public final long sn;

    public final String unit;

    public String answer;

    public String thought;

    public JSONObject resultPayload;

    public Usage usage;

    public long timestamp;

    public ComplexContext context;

    public MultimodalOutput(long sn, String unit, String answer, String thought, JSONObject resultPayload) {
        this.sn = sn;
        this.unit = unit;
        this.answer = answer;
        this.thought = thought;
        this.resultPayload = resultPayload;
        this.timestamp = System.currentTimeMillis();
    }

    public MultimodalOutput(JSONObject json) {
        if (json.has("sn")) {
            this.sn = json.getLong("sn");
        }
        else {
            this.sn = Utils.generateSerialNumber();
        }

        if (json.has("unit")) {
            this.unit = json.getString("unit");
        }
        else {
            this.unit = "";
        }

        if (json.has("answer")) {
            this.answer = json.getString("answer");
        }
        else {
            this.answer = "";
        }

        if (json.has("thought")) {
            this.thought = json.getString("thought");
        }
        else {
            this.thought = "";
        }

        if (json.has("resultPayload")) {
            this.resultPayload = json.getJSONObject("resultPayload");
        }

        if (json.has("usage")) {
            this.usage = new Usage(json.getJSONObject("usage"));
        }

        if (json.has("timestamp")) {
            this.timestamp = json.getLong("timestamp");
        }
        else {
            this.timestamp = System.currentTimeMillis();
        }

        if (json.has("context")) {
            this.context = new ComplexContext(json.getJSONObject("context"));
        }
    }

    public MultimodalOutput(GeneratingRecord record) {
        this.sn = record.sn;
        this.unit = record.unit;
        this.answer = record.answer;
        this.thought = record.thought;
        this.resultPayload = record.resultPayload;
        this.context = record.context;
        this.timestamp = record.timestamp;
    }

    public GeneratingRecord toRecord() {
        return new GeneratingRecord(this.sn,
                this.unit, "", this.answer, this.thought, this.resultPayload,
                this.timestamp, this.context);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("unit", this.unit);
        json.put("answer", this.answer);
        json.put("thought", this.thought);
        json.put("timestamp", this.timestamp);

        if (null != this.resultPayload) {
            json.put("resultPayload", this.resultPayload);
        }

        if (null != this.usage) {
            json.put("usage", this.usage.toJSON());
        }

        if (null != this.context) {
            json.put("context", this.context.toJSON());
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = this.toJSON();
        if (json.has("context")) {
            json.remove("context");
        }
        return json;
    }
}
