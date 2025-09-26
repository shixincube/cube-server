/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.fileprocessor;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.CachedQueueExecutor;
import cube.common.action.FileProcessorAction;
import cube.core.AbstractCellet;
import cube.core.Kernel;
import cube.service.fileprocessor.task.CancelWorkflowTask;
import cube.service.fileprocessor.task.DetectObjectTask;
import cube.service.fileprocessor.task.SteganographicTask;
import cube.service.fileprocessor.task.SubmitWorkflowTask;

import java.util.concurrent.ExecutorService;

/**
 * 文件处理服务的 Cellet 服务。
 */
public class FileProcessorServiceCellet extends AbstractCellet {

    private FileProcessorService service = null;

    private ExecutorService executor = null;

    public FileProcessorServiceCellet() {
        super(FileProcessorService.NAME);
    }

    public FileProcessorService getService() {
        return this.service;
    }

    @Override
    public boolean install() {
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(64);

        this.service = new FileProcessorService(this.executor, this);

        Kernel kernel = (Kernel) this.nucleus.getParameter("kernel");
        kernel.installModule(this.getName(), this.service);

        return true;
    }

    @Override
    public void uninstall() {
        Kernel kernel = (Kernel) this.nucleus.getParameter("kernel");
        kernel.uninstallModule(this.getName());

        this.executor.shutdown();
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        ActionDialect dialect = DialectFactory.getInstance().createActionDialect(primitive);
        String action = dialect.getName();

        if (FileProcessorAction.SubmitWorkflow.name.equals(action)) {
            this.executor.execute(new SubmitWorkflowTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileProcessorAction.CancelWorkflow.name.equals(action)) {
            this.executor.execute(new CancelWorkflowTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileProcessorAction.DetectObject.name.equals(action)) {
            this.executor.execute(new DetectObjectTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileProcessorAction.Steganographic.name.equals(action)) {
            this.executor.execute(new SteganographicTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
    }
}
