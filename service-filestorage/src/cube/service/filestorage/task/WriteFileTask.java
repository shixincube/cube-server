/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.filestorage.task;

import cell.core.cellet.Cellet;
import cell.core.talk.PrimitiveInputStream;
import cell.core.talk.TalkContext;
import cube.core.Kernel;
import cube.service.filestorage.FileStorageService;

import java.io.IOException;

/**
 * 写文件任务。
 */
public class WriteFileTask implements Runnable {

    /**
     * 框架的内核实例。
     */
    protected Kernel kernel;

    protected TalkContext talkContext;

    protected PrimitiveInputStream inputStream;

    protected FileStorageService service;

    public WriteFileTask(Cellet cellet, TalkContext talkContext, PrimitiveInputStream inputStream) {
        this.kernel = (Kernel) cellet.getNucleus().getParameter("kernel");
        this.talkContext = talkContext;
        this.inputStream = inputStream;
    }

    @Override
    public void run() {
        this.service = (FileStorageService) this.kernel.getModule(FileStorageService.NAME);
        this.service.writeFile(this.inputStream.getName(), this.inputStream);

        try {
            this.inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
