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

package cube.file.event;

import cube.common.JSONable;
import cube.file.OperationWorkflow;
import cube.file.OperationWork;
import org.json.JSONObject;

import java.io.File;

/**
 * 工作流事件。
 */
public class FileWorkflowEvent implements JSONable {

    private String name;

    private OperationWorkflow workflow;

    private OperationWork work;

    public File resultFile;

    public FileWorkflowEvent(String name, OperationWorkflow workflow, OperationWork work) {
        this.name = name;
        this.workflow = workflow;
        this.work = work;
    }

    public FileWorkflowEvent(JSONObject json) {
        this.name = json.getString("name");
        this.workflow = new OperationWorkflow(json.getJSONObject("workflow"));
        if (json.has("work")) {
            this.work = new OperationWork(json.getJSONObject("work"));
        }
    }

    public String getName() {
        return this.name;
    }

    public OperationWorkflow getWorkflow() {
        return this.workflow;
    }

    public OperationWork getWork() {
        return this.work;
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
