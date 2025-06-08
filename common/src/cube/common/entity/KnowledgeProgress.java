/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cell.util.Utils;
import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 知识库进度。
 */
public class KnowledgeProgress implements JSONable {

    private final long sn;

    private long startTime;

    private long endTime;

    private int totalDocs;

    private int processedDocs;

    private KnowledgeDocument processingDoc;

    private int stateCode;

    public KnowledgeProgress(int stateCode) {
        this.sn = Utils.generateSerialNumber();
        this.startTime = System.currentTimeMillis();
        this.totalDocs = 0;
        this.processedDocs = 0;
        this.stateCode = stateCode;
    }

    public KnowledgeProgress(JSONObject json) {
        this.sn = json.getLong("sn");
        this.startTime = json.getLong("startTime");
        this.endTime = json.getLong("endTime");
        this.totalDocs = json.getInt("totalDocs");
        this.processedDocs = json.getInt("processedDocs");
        if (json.has("processingDoc")) {
            this.processingDoc = new KnowledgeDocument(json.getJSONObject("processingDoc"));
        }
        this.stateCode = json.getInt("stateCode");
    }

    public long getSN() {
        return this.sn;
    }

    public void setTotalDocs(int value) {
        this.totalDocs = value;
    }

    public int getProcessedDocs() {
        return this.processedDocs;
    }

    public void setProcessedDocs(int value) {
        this.processedDocs = value;
    }

    public void setProcessingDoc(KnowledgeDocument doc) {
        this.processingDoc = doc;
    }

    public void setStateCode(int code) {
        this.stateCode = code;
    }

    public void setEndTime(long time) {
        this.endTime = time;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("startTime", this.startTime);
        json.put("endTime", this.endTime);
        json.put("totalDocs", this.totalDocs);
        json.put("processedDocs", this.processedDocs);
        if (null != this.processingDoc) {
            json.put("activatingDoc", this.processingDoc.toJSON());
        }
        json.put("stateCode", this.stateCode);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
