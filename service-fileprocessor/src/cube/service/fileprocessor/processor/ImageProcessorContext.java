/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

import cube.common.action.FileProcessorAction;
import cube.common.entity.FileLabel;
import cube.file.EliminateColorOperation;
import cube.file.FileOperation;
import cube.file.ImageOperation;
import cube.file.ReverseColorOperation;
import org.json.JSONObject;

/**
 * 图像处理上下文。
 */
public class ImageProcessorContext extends ProcessorContext {

    private ImageOperation imageOperation;

    private FileLabel inputFileLabel;

    public ImageProcessorContext() {
    }

    public ImageProcessorContext(JSONObject json) {
        super(json);
    }

    public void parseOperation(JSONObject parameterJSON) {
        String operation = parameterJSON.getString("operation");
        if (EliminateColorOperation.Operation.equals(operation)) {
            this.imageOperation = new EliminateColorOperation(parameterJSON);
        }
        else if (ReverseColorOperation.Operation.equals(operation)) {
            this.imageOperation = new ReverseColorOperation(parameterJSON);
        }
    }

    public void parseOperation(FileOperation fileOperation) {
        this.imageOperation = (ImageOperation) fileOperation;
    }

    public ImageOperation getImageOperation() {
        return this.imageOperation;
    }

    public void setInputFileLabel(FileLabel fileLabel) {
        this.inputFileLabel = fileLabel;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON(FileProcessorAction.Image.name);

        if (null != this.inputFileLabel) {
            json.put("inputFileLabel", this.inputFileLabel.toCompactJSON());
        }

        if (null != this.imageOperation) {
            json.put("operation", this.imageOperation.toJSON());
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
