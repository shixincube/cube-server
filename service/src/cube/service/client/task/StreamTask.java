/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.client.task;

import cell.core.talk.PrimitiveInputStream;
import cell.core.talk.TalkContext;
import cube.core.AbstractModule;
import cube.service.client.ClientCellet;

/**
 * 处理流任务。
 */
public class StreamTask extends ClientTask {

    private PrimitiveInputStream inputStream;

    public StreamTask(ClientCellet cellet, TalkContext talkContext, PrimitiveInputStream inputStream) {
        super(cellet, talkContext, null);
        this.inputStream = inputStream;
    }

    @Override
    public void run() {
        AbstractModule module = this.getFileStorageModule();
        module.notify(this.inputStream);
    }
}
