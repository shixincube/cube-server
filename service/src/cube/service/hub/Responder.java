/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.hub;

import cell.core.cellet.Cellet;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.hub.event.Event;
import cube.hub.signal.Signal;
import org.json.JSONObject;

/**
 * 应答机。
 */
public class Responder {

    private final static String NotifierKey = "_notifier";

    private final static String PerformerKey = "_performer";

    private Cellet cellet;

    private TalkContext talkContext;

    private JSONObject notifier;
    private String name;

    public Responder(ActionDialect request, Cellet cellet, TalkContext talkContext) {
        this.notifier = request.getParamAsJson(NotifierKey);
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

    public void respond(int code, JSONObject data) {
        ActionDialect actionDialect = new ActionDialect(this.name);
        if (null != this.notifier) {
            actionDialect.addParam(NotifierKey, this.notifier);
        }
        actionDialect.addParam("code", code);
        actionDialect.addParam("data", data);
        this.cellet.speak(this.talkContext, actionDialect);
    }

    public void respondDispatcher(long sn, int code, Signal signal) {
        ActionDialect actionDialect = new ActionDialect(this.name);
        actionDialect.addParam(PerformerKey, createPerformer(sn));
        actionDialect.addParam("code", code);
        actionDialect.addParam("signal", signal.toJSON());
        this.cellet.speak(this.talkContext, actionDialect);
    }

    public void respondDispatcher(long sn, int code, Event event) {
        ActionDialect actionDialect = new ActionDialect(this.name);
        actionDialect.addParam(PerformerKey, createPerformer(sn));
        actionDialect.addParam("code", code);
        actionDialect.addParam("event", event.toJSON());
        this.cellet.speak(this.talkContext, actionDialect);
    }

    private JSONObject createPerformer(long sn) {
        JSONObject json = new JSONObject();
        json.put("sn", sn);
        json.put("ts", System.currentTimeMillis());
        return json;
    }
}
