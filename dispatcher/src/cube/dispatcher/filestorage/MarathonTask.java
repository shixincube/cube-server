/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.filestorage;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.common.StateCode;
import cube.dispatcher.DispatcherTask;
import cube.dispatcher.Performer;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 需要较长时长的任务。
 */
public class MarathonTask extends DispatcherTask {

    public final static int sMaxThreads = 16;

    private final static AtomicInteger sNumThreads = new AtomicInteger(0);

    private long timeout = 5 * 60 * 1000;

    public MarathonTask(FileStorageCellet cellet, TalkContext talkContext, Primitive primitive
            , Performer performer) {
        super(cellet, talkContext, primitive, performer);
    }

    public boolean start() {
        if (sNumThreads.get() >= sMaxThreads) {
            return false;
        }

        sNumThreads.incrementAndGet();

        (new Thread() {
            @Override
            public void run() {
                try {
                    MarathonTask.this.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                MarathonTask.this.markResponseTime();

                sNumThreads.decrementAndGet();
            }
        }).start();

        return true;
    }

    public void responseBusy() {
        ActionDialect response = this.makeResponse(new JSONObject(), StateCode.SystemBusy, "Busy");
        this.cellet.speak(this.talkContext, response);
    }

    @Override
    public void run() {
        String tokenCode = this.getTokenCode(this.getAction());
        if (null == tokenCode) {
            // 无令牌码
            ActionDialect response = this.makeResponse(new JSONObject(), StateCode.NoAuthToken, "No token code");
            this.cellet.speak(this.talkContext, response);
            return;
        }

        ActionDialect response = this.performer.syncTransmit(this.talkContext, this.cellet.getName(), this.getAction(),
                this.timeout);

        if (null == response) {
            response = this.makeResponse(this.getRequest().data, StateCode.GatewayError, "Service failed");
        }
        else {
            response = this.makeResponse(response);
        }

        this.cellet.speak(this.talkContext, response);
    }
}
