/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
