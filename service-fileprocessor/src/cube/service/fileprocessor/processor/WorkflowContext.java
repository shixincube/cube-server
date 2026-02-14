/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.fileprocessor.processor;

import cube.processor.ProcessorContext;
import org.json.JSONObject;

/**
 * 工作流上下文。
 */
public class WorkflowContext extends ProcessorContext {

    private String action;

    public WorkflowContext(String action) {
        this.action = action;
    }

    public WorkflowContext(JSONObject json) {
        super(json);
        this.action = json.getString("process");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON(this.action);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
