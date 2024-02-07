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

package cube.service.fileprocessor;

import cube.file.OperationWorkflow;
import cube.file.OperationWork;
import cube.plugin.PluginContext;

import java.io.File;

/**
 * 工作流插件上下文。
 */
public class WorkflowPluginContext extends PluginContext {

    private OperationWorkflow workflow;

    private OperationWork work;

    private File resultFile;

    public WorkflowPluginContext(OperationWorkflow workflow) {
        super();
        this.workflow = workflow;
    }

    public WorkflowPluginContext(OperationWorkflow workflow, File resultFile) {
        super();
        this.workflow = workflow;
        this.resultFile = resultFile;
    }

    public WorkflowPluginContext(OperationWorkflow workflow, OperationWork work) {
        super();
        this.workflow = workflow;
        this.work = work;
    }

    public OperationWorkflow getWorkflow() {
        return this.workflow;
    }

    public OperationWork getWork() {
        return this.work;
    }

    public File getResultFile() {
        return this.resultFile;
    }

    @Override
    public Object get(String name) {
        if (name.equals("workflow")) {
            return this.workflow;
        }
        else if (name.equals("work")) {
            return this.work;
        }
        else if (name.equals("resultFile")) {
            return this.resultFile;
        }

        return null;
    }

    @Override
    public void set(String name, Object value) {
        if (name.equals("workflow")) {
            this.workflow = (OperationWorkflow) value;
        }
        else if (name.equals("work")) {
            this.work = (OperationWork) value;
        }
        else if (name.equals("resultFile")) {
            this.resultFile = (File) value;
        }
    }
}
