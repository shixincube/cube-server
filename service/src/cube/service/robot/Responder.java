/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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
