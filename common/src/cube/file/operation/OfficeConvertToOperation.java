/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
