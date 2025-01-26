/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.robot;

import cell.core.cellet.Cellet;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import org.json.JSONObject;

/**
 * 应答机。
 */
public class Responder {

    private final static String NotifierKey = "_notifier";

    private final static String PerformerKey = "_performer";

    private Cellet cellet;

    private TalkContext talkContext;

    private long performerSN;

    private JSONObject notifier;

    private String name;

    public Responder(ActionDialect request, Cellet cellet, TalkContext talkContext) {
        this.notifier = request.getParamAsJson(NotifierKey);

        if (request.containsParam(PerformerKey)) {
            this.performerSN = request.getParamAsJson(PerformerKey).getLong("sn");
        }

        this.name = request.getName();

        this.cellet = cellet;
        this.talkContext = talkContext;
    }

    public TalkContext getTalkContext() {
        return this.talkContext;
    }

    public String getClientAddress() {
        return this.talkContext.getSessionHost();
    }

    public boolean respond(int code, JSONObject data) {
        ActionDialect actionDialect = new ActionDialect(this.name);
        if (null != this.notifier) {
            actionDialect.addParam(NotifierKey, this.notifier);
        }
        else {
            actionDialect.addParam(PerformerKey, createPerformer(this.performerSN));
        }
        actionDialect.addParam("code", code);
        actionDialect.addParam("data", data);
        return this.cellet.speak(this.talkContext, actionDialect);
    }

    private JSONObject createPerformer(long sn) {
        JSONObject json = new JSONObject();
        json.put("sn", sn);
        json.put("ts", System.currentTimeMillis());
        return json;
    }
}
