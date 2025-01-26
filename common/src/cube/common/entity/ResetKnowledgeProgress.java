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
 * 重置知识库进度。
 */
public class ResetKnowledgeProgress implements JSONable {

    public final static String PROGRESS_START = "Start";

    public final static String PROGRESS_BACKUP_STORE = "BackupStore";

    public final static String PROGRESS_DELETE_STORE = "DeleteStore";

    public final static String PROGRESS_ACTIVATE_DOC = "ActivateDoc";

    public final static String PROGRESS_END = "End";

    private final long sn;

    private String progress;

    private long startTime;

    private long endTime;

    private int totalDocs;

    private int processedDocs;

    private KnowledgeDoc activatingDoc;

    private int stateCode;

    public ResetKnowledgeProgress() {
        this.sn = Utils.generateSerialNumber();
        this.progress = PROGRESS_START;
        this.startTime = System.currentTimeMillis();
        this.totalDocs = -1;
        this.processedDocs = -1;
    }

    public ResetKnowledgeProgress(JSONObject json) {
        this.sn = json.getLong("sn");
        this.progress = json.getString("progress");
        this.startTime = json.getLong("startTime");
        this.endTime = json.getLong("endTime");
        this.totalDocs = json.getInt("totalDocs");
        this.processedDocs = json.getInt("processedDocs");
        if (json.has("activatingDoc")) {
            this.activatingDoc = new KnowledgeDoc(json.getJSONObject("activatingDoc"));
        }
        this.stateCode = json.getInt("stateCode");
    }

    public long getSN() {
        return this.sn;
    }

    public void setProgress(String value) {
        this.progress = value;
        if (PROGRESS_END.equalsIgnoreCase(value)) {
            this.endTime = System.currentTimeMillis();
        }
    }

    public String getProgress() {
        return this.progress;
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

    public void setActivatingDoc(KnowledgeDoc doc) {
        this.activatingDoc = doc;
    }

    public void setStateCode(int code) {
        this.stateCode = code;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("progress", this.progress);
        json.put("startTime", this.startTime);
        json.put("endTime", this.endTime);
        json.put("totalDocs", this.totalDocs);
        json.put("processedDocs", this.processedDocs);
        if (null != this.activatingDoc) {
            json.put("activatingDoc", this.activatingDoc.toJSON());
        }
        json.put("stateCode", this.stateCode);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
