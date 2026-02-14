/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.fileprocessor.processor;

import cube.file.operation.OfficeConvertToOperation;
import cube.processor.ProcessorContext;
import org.json.JSONObject;

/**
 * 文档转换处理器上下文。
 */
public class OfficeConvertToProcessorContext extends ProcessorContext {

    private OfficeConvertToOperation operation;

    public OfficeConvertToProcessorContext(OfficeConvertToOperation operation) {
        super();
        this.operation = operation;
    }

    public OfficeConvertToProcessorContext(JSONObject json) {
        super(json);
        this.operation = new OfficeConvertToOperation(json.getJSONObject("operation"));
    }

    public OfficeConvertToOperation getOperation() {
        return this.operation;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON(this.operation.getProcessAction());
        json.put("operation", this.operation.toJSON());
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
