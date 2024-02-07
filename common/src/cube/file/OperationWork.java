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

package cube.file;

import cube.common.JSONable;
import org.json.JSONObject;

import java.io.File;
import java.util.List;

/**
 * 文件操作工作。
 */
public class OperationWork implements JSONable {

    private FileOperation fileOperation;

    private List<File> inputList;

    private List<File> outputList;

    private FileProcessResult processResult;

    public OperationWork(FileOperation fileOperation) {
        this.fileOperation = fileOperation;
    }

    public OperationWork(JSONObject json) {
        this.fileOperation = FileOperationHelper.parseFileOperation(json.getJSONObject("operation"));
    }

    public void setOperation(FileOperation fileOperation) {
        this.fileOperation = fileOperation;
    }

    public FileOperation getFileOperation() {
        return this.fileOperation;
    }

    public void setInputFiles(List<File> files) {
        this.inputList = files;
    }

    public List<File> getInputFiles() {
        return this.inputList;
    }

    public void setOutputFiles(List<File> files) {
        this.outputList = files;
    }

    public List<File> getOutputFiles() {
        return this.outputList;
    }

    public void setProcessResult(FileProcessResult result) {
        this.processResult = result;
    }

    public FileProcessResult getProcessResult() {
        return this.processResult;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("operation", this.fileOperation.toJSON());
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
