/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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
import cube.common.UniqueKey;
import cube.common.action.ClientAction;
import cube.common.entity.Entity;
import cube.common.entity.Message;
import cube.service.client.event.MessageReceiveEvent;
import cube.service.client.event.MessageSendEvent;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端管理实体。
 */
public class ServerClient extends Entity {

    private Cellet cellet;

    protected TalkContext talkContext;

    protected List<String> events;

    // Key : 实体唯一键
    protected Map<String, MessageReceiveEvent> messageReceiveEvents;

    // Key : 实体唯一键
    protected Map<String, MessageSendEvent> messageSendEvents;

    private Timer disableTimer;

    public ServerClient(Long id, Cellet cellet, TalkContext talkContext) {
        super(id);
        this.cellet = cellet;
        this.talkContext = talkContext;
        this.events = new ArrayList<>();
        this.messageReceiveEvents = new ConcurrentHashMap<>();
        this.messageSendEvents = new ConcurrentHashMap<>();
    }

    public void resetTalkContext(TalkContext talkContext) {
        this.talkContext = talkContext;

        if (null != this.disableTimer) {
            this.disableTimer.cancel();
            this.disableTimer = null;
        }
    }

    public TalkContext getTalkContext() {
        return this.talkContext;
    }

    protected void disable(final TimeoutCallback callback) {
        this.talkContext = null;

        if (null == this.disableTimer) {
            this.disableTimer = new Timer();
            this.disableTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    callback.on(ServerClient.this);
                }
            }, 60 * 1000);
        }
    }

    /**
     * 添加事件。
     *
     * @param event
     */
    public void addEvent(String event) {
        if (!this.events.contains(event)) {
            this.events.add(event);
        }
    }

    /**
     * 移除事件。
     *
     * @param event
     */
    public void removeEvent(String event) {
        this.events.remove(event);
    }

    /**
     * 是否包含指定名称的事件。
     *
     * @param event
     * @return
     */
    public boolean hasEvent(String event) {
        return this.events.contains(event);
    }

    /**
     * 添加事件。
     *
     * @param event
     */
    public void addEvent(MessageReceiveEvent event) {
        this.addEvent(MessageReceiveEvent.NAME);

        if (this.messageReceiveEvents.containsKey(event.getUniqueKey())) {
            return;
        }

        this.messageReceiveEvents.put(event.getUniqueKey(), event);
    }

    /**
     * 添加事件。
     *
     * @param event
     */
    public void addEvent(MessageSendEvent event) {
        this.addEvent(MessageSendEvent.NAME);

        if (this.messageSendEvents.containsKey(event.getUniqueKey())) {
            return;
        }

        this.messageSendEvents.put(event.getUniqueKey(), event);
    }

    /**
     * 移除事件。
     *
     * @param event
     */
    public void removeEvent(MessageReceiveEvent event) {
        this.messageReceiveEvents.remove(event.getUniqueKey());

        if (this.messageReceiveEvents.isEmpty()) {
            this.removeEvent(MessageReceiveEvent.NAME);
        }
    }

    /**
     * 移除事件。
     *
     * @param event
     */
    public void removeEvent(MessageSendEvent event) {
        this.messageSendEvents.remove(event.getUniqueKey());

        if (this.messageSendEvents.isEmpty()) {
            this.removeEvent(MessageSendEvent.NAME);
        }
    }

    /**
     *
     * @param message
     * @return
     */
    public MessageReceiveEvent queryReceiveEvent(Message message) {
        String key = null;
        long sourceId = message.getSource().longValue();
        if (sourceId == 0) {
            key = UniqueKey.make(message.getTo(), message.getDomain().getName());
        }
        else {
            key = UniqueKey.make(sourceId, message.getDomain().getName());
        }

        MessageReceiveEvent event = this.messageReceiveEvents.get(key);
        if (null != event) {
            event.setMessage(message);
            return event;
        }

        return null;
    }

    /**
     *
     * @param message
     * @return
     */
    public MessageSendEvent querySendEvent(Message message) {
        String key = UniqueKey.make(message.getFrom(), message.getDomain().getName());

        MessageSendEvent event = this.messageSendEvents.get(key);
        if (null != event) {
            event.setMessage(message);
            return event;
        }

        return null;
    }

    public void sendEvent(String eventName, JSONObject data) {
        if (null == this.talkContext) {
            return;
        }

        ActionDialect actionDialect = new ActionDialect(ClientAction.NotifyEvent.name);
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
