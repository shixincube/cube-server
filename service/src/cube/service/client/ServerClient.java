/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

package cube.service.client;

import cell.core.cellet.Cellet;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.common.entity.Entity;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端管理实体。
 */
public class ServerClient extends Entity {

    private Long id;

    private Cellet cellet;

    protected TalkContext talkContext;

    protected List<String> events;

    public ServerClient(Long id, Cellet cellet, TalkContext talkContext) {
        this.id = id;
        this.cellet = cellet;
        this.talkContext = talkContext;
        this.events = new ArrayList<>();
    }

    public void setTalkContext(TalkContext talkContext) {
        this.talkContext = talkContext;
    }

    public TalkContext getTalkContext() {
        return this.talkContext;
    }

    protected void disable() {
        this.talkContext = null;
        this.events.clear();
    }

    public void addEvent(String event) {
        if (this.events.contains(event)) {
            return;
        }

        this.events.add(event);
    }

    public void removeEvent(String event) {
        this.events.remove(event);
    }

    public boolean hasEvent(String event) {
        return this.events.contains(event);
    }

    public void sendEvent(String eventName, JSONObject data) {
        if (null == this.talkContext) {
            return;
        }

        ActionDialect actionDialect = new ActionDialect(Actions.NotifyEvent.name);
        actionDialect.addParam("event", eventName);
        actionDialect.addParam("data", data);
        this.cellet.speak(this.talkContext, actionDialect);
    }

    @Override
    public JSONObject toJSON() {
        return null;
    }

    @Override
    public JSONObject toCompactJSON() {
        return null;
    }
}
