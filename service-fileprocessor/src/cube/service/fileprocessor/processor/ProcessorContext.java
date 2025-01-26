/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.fileprocessor.processor;

import cube.common.JSONable;
import cube.common.entity.FileResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 处理器上下文。
 */
public abstract class ProcessorContext implements JSONable {

    private boolean successful = false;

    private List<String> stdOutput;

    private List<FileResult> resultList;

    protected ProcessorContext() {
        this.resultList = new ArrayList<>();
    }

    protected ProcessorContext(JSONObject json) {
        this.successful = json.getBoolean("success");
        this.resultList = new ArrayList<>();

        if (json.has("resultList")) {
            JSONArray array = json.getJSONArray("resultList");
            for (int i = 0; i < array.length(); ++i) {
                this.resultList.add(new FileResult(array.getJSONObject(i)));
            }
        }
    }

    public void setSuccessful(boolean value) {
        this.successful = value;
    }

    public boolean isSuccessful() {
        return this.successful;
    }

    public void addResult(FileResult result) {
        this.resultList.add(result);
    }

    public List<FileResult> getResultList() {
        return this.resultList;
    }

    public synchronized void appendStdOutput(String line) {
        if (null == this.stdOutput) {
            this.stdOutput = new ArrayList<>();
        }

        this.stdOutput.add(line);
    }

    protected List<String> getStdOutput() {
        return this.stdOutput;
    }

    public JSONObject toJSON(String process) {
        JSONObject json = new JSONObject();

        json.put("process", process);
        json.put("success", this.successful);

        if (!this.resultList.isEmpty()) {
            JSONArray array = new JSONArray();
            for (FileResult result : this.resultList) {
                array.put(result.toJSON());
            }
            json.put("resultList", array);
        }

        return json;
    }
}
