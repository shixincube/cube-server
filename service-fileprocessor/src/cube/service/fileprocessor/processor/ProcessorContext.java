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

import cube.common.JSONable;
import cube.common.entity.FileResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 处理器上下文。
 */
public abstract class ProcessorContext implements JSONable {

    private boolean successful = false;

    private List<String> stdOutput;

    private List<FileResult> resultList;

    protected ProcessorContext() {
        this.resultList = new ArrayList<>();
    }

    protected ProcessorContext(JSONObject json) {
        this.successful = json.getBoolean("success");
        this.resultList = new ArrayList<>();

        if (json.has("resultList")) {
            JSONArray array = json.getJSONArray("resultList");
            for (int i = 0; i < array.length(); ++i) {
                this.resultList.add(new FileResult(array.getJSONObject(i)));
            }
        }
    }

    public void setSuccessful(boolean value) {
        this.successful = value;
    }

    public boolean isSuccessful() {
        return this.successful;
    }

    public void addResult(FileResult result) {
        this.resultList.add(result);
    }

    public List<FileResult> getResultList() {
        return this.resultList;
    }

    public synchronized void appendStdOutput(String line) {
        if (null == this.stdOutput) {
            this.stdOutput = new ArrayList<>();
        }

        this.stdOutput.add(line);
    }

    protected List<String> getStdOutput() {
        return this.stdOutput;
    }

    public JSONObject toJSON(String process) {
        JSONObject json = new JSONObject();

        json.put("process", process);
        json.put("success", this.successful);

        if (!this.resultList.isEmpty()) {
            JSONArray array = new JSONArray();
            for (FileResult result : this.resultList) {
                array.put(result.toJSON());
            }
            json.put("resultList", array);
        }

        return json;
    }
}
