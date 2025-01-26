/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.client.task;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.common.state.AuthStateCode;
import cube.service.client.ClientCellet;

/**
 * 不支持的操作。
 */
public class UnsupportedActionTask extends ClientTask {

    public UnsupportedActionTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        ActionDialect response = new ActionDialect(this.actionDialect.getName());
        copyNotifier(response);

        response.addParam("code", AuthStateCode.Failure.code);
        this.cellet.speak(this.talkContext, response);
    }
}
