/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.fileprocessor;

import cube.plugin.PluginSystem;

/**
 * 处理器插件。
 */
public class ProcessorPluginSystem extends PluginSystem<ProcessorHook> {

    public ProcessorPluginSystem(FileProcessorServiceCellet cellet) {
        this.build(cellet);
    }

    public ProcessorHook getWorkflowStartedHook() {
        return this.getHook(ProcessorHook.WorkflowStarted);
    }

    public ProcessorHook getWorkflowStoppedHook() {
        return this.getHook(ProcessorHook.WorkflowStopped);
    }

    public ProcessorHook getWorkBegunHook() {
        return this.getHook(ProcessorHook.WorkBegun);
    }

    public ProcessorHook getWorkEndedHook() {
        return this.getHook(ProcessorHook.WorkEnded);
    }

    private void build(FileProcessorServiceCellet cellet) {
        ProcessorHook hook = new ProcessorHook(ProcessorHook.WorkflowStarted, cellet);
        this.addHook(hook);

        hook = new ProcessorHook(ProcessorHook.WorkflowStopped, cellet);
        this.addHook(hook);

        hook = new ProcessorHook(ProcessorHook.WorkBegun, cellet);
        this.addHook(hook);

        hook = new ProcessorHook(ProcessorHook.WorkEnded, cellet);
        this.addHook(hook);
    }
}
