/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc;

import cube.common.JSONable;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TaskDescriptor implements JSONable {

    public final long id;

    public final long contactId;

    public final String domain;

    public final String token;

    public final String task;

    public final long timestamp;

    private int inputTokens;

    private int outputTokens;

    private List<String> fileCodeList;

    private long unitId;

    public TaskDescriptor(long id, long contactId, String domain, String token, String task, long timestamp) {
        this.id = id;
        this.contactId = contactId;
        this.domain = domain;
        this.token = token;
        this.task = task;
        this.timestamp = timestamp;
    }

//    public TaskDescriptor(JSONObject json) {
//    }

    public int getInputTokens() {
        return this.inputTokens;
    }

    public int getOutputTokens() {
        return this.outputTokens;
    }

    public void addFileCode(String fileCode) {
        if (null == this.fileCodeList) {
            this.fileCodeList = new ArrayList<>();
        }
        this.fileCodeList.add(fileCode);
    }

    public String getFileCode() {
        return (null != this.fileCodeList) ? this.fileCodeList.get(0) : null;
    }

    public String getFileCode2() {
        return (null != this.fileCodeList && this.fileCodeList.size() >= 2) ? this.fileCodeList.get(1) : null;
    }

    public String getFileCode3() {
        return (null != this.fileCodeList && this.fileCodeList.size() >= 3) ? this.fileCodeList.get(2) : null;
    }

    public String getFileCode4() {
        return (null != this.fileCodeList && this.fileCodeList.size() >= 4) ? this.fileCodeList.get(3) : null;
    }

    public String getFileCode5() {
        return (null != this.fileCodeList && this.fileCodeList.size() >= 5) ? this.fileCodeList.get(4) : null;
    }

    public void setUnitId(long unitId) {
        this.unitId = unitId;
    }

    public long getUnitId() {
        return this.unitId;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
