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

package cube.service.fileprocessor.processor;

import cube.file.operation.OfficeConvertToOperation;
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
