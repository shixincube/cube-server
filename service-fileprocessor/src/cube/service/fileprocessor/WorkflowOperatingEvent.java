/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.fileprocessor;

import cube.common.JSONable;
import cube.file.OperationWork;
import cube.file.OperationWorkflow;
import org.json.JSONObject;

/**
 * 工作流操作事件。
 */
public class WorkflowOperatingEvent implements JSONable {

    /**
     * 事件名称。
     */
    private String name;

    private OperationWorkflow workflow;

    private OperationWork work;

    public WorkflowOperatingEvent(String name, OperationWorkflow workflow) {
        this.name = name;
        this.workflow = workflow;
    }

    public String getName() {
        return this.name;
    }

    public void setWork(OperationWork work) {
        this.work = work;
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
