/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.cv;

import cell.core.talk.dialect.ActionDialect;
import org.json.JSONObject;

/**
 * 应答机。
 */
public class Responder {

    public final static String NotifierKey = "_notifier";

    private long sn;

    private JSONObject notifier;

    private ActionDialect response;

    public Responder(long sn, ActionDialect actionDialect) {
        this.sn = sn;
        this.notifier = createNotifier();
        actionDialect.addParam(Responder.NotifierKey, this.notifier);
    }

    public long getSN() {
        return this.sn;
    }

    public ActionDialect waitingFor(long timeout) {
        synchronized (this.notifier) {
            try {
                this.notifier.wait(timeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return this.response;
    }

    public void notifyResponse(ActionDialect response) {
        response.removeParam(Responder.NotifierKey);

        this.response = response;
        synchronized (this.notifier) {
            this.notifier.notify();
        }
    }

    public void finish() {
        synchronized (this.notifier) {
            this.notifier.notify();
        }
    }

    public boolean isResponse(ActionDialect response) {
        JSONObject notifier = response.getParamAsJson(Responder.NotifierKey);
        if (notifier.getLong("sn") == this.sn) {
            return true;
        }

        return false;
    }

    private JSONObject createNotifier() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("ts", System.currentTimeMillis());
        return json;
    }
}
