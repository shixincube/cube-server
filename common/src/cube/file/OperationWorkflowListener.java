/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.file;

/**
 * 文件操作工作流监听器。
 */
public interface OperationWorkflowListener {

    /**
     * 工作流已开始执行时回调。
     *
     * @param workflow
     */
    void onWorkflowStarted(OperationWorkflow workflow);

    /**
     * 工作流已执行结束时回调。
     *
     * @param workflow
     */
    void onWorkflowStopped(OperationWorkflow workflow);
}
