/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.file.operation;

import cube.common.action.FileProcessorAction;
import cube.file.FileOperation;
import org.json.JSONObject;

/**
 * 办公文档转换操作。
 */
public class OfficeConvertToOperation implements FileOperation {

    public final static String OUTPUT_FORMAT_PDF = "pdf";

    public final static String OUTPUT_FORMAT_PNG = "png";

    public final static String OUTPUT_FORMAT_TEXT = "txt";

    private String outputFormat;

    public OfficeConvertToOperation(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public OfficeConvertToOperation(JSONObject json) {
        this.outputFormat = json.getString("format");
    }

    public String getOutputFormat() {
        return this.outputFormat;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("process", this.getProcessAction());
        json.put("format", this.outputFormat);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    @Override
    public String getProcessAction() {
        return FileProcessorAction.OfficeConvertTo.name;
    }
}
