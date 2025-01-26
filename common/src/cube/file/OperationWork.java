/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
