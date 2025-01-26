/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.file.event;

import cube.common.JSONable;
import cube.file.OperationWorkflow;
import cube.file.OperationWork;
import org.json.JSONObject;

import java.io.File;

/**
 * 工作流事件。
 */
public class FileWorkflowEvent implements JSONable {

    private String name;

    private OperationWorkflow workflow;

    private OperationWork work;

    public File resultFile;

    public FileWorkflowEvent(String name, OperationWorkflow workflow, OperationWork work) {
        this.name = name;
        this.workflow = workflow;
        this.work = work;
    }

    public FileWorkflowEvent(JSONObject json) {
        this.name = json.getString("name");
        this.workflow = new OperationWorkflow(json.getJSONObject("workflow"));
        if (json.has("work")) {
            this.work = new OperationWork(json.getJSONObject("work"));
        }
    }

    public String getName() {
        return this.name;
    }

    public OperationWorkflow getWorkflow() {
        return this.workflow;
    }

    public OperationWork getWork() {
        return this.work;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("workflow", this.workflow.toJSON());
        if (null != this.work) {
            json.put("work", this.work.toJSON());
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
