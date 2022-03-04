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
