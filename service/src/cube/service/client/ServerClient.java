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
import cube.common.entity.Contact;
import cube.common.entity.Entity;
import cube.common.entity.Message;
import cube.service.client.event.MessageReceiveEvent;
import cube.service.client.event.MessageSendEvent;
import org.json.JSONObject;

import java.util.*;

/**
 * 客户端管理实体。
 */
public class ServerClient extends Entity {

    private Cellet cellet;

    protected TalkContext talkContext;

    protected List<String> events;

    protected List<MessageReceiveEvent> messageReceiveEvents;

    protected List<MessageSendEvent> messageSendEvents;

    private Timer disableTimer;

    public ServerClient(Long id, Cellet cellet, TalkContext talkContext) {
        super(id);
        this.cellet = cellet;
        this.talkContext = talkContext;
        this.events = new ArrayList<>();
        this.messageReceiveEvents = new Vector<>();
        this.messageSendEvents = new Vector<>();
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

        if (this.messageReceiveEvents.contains(event)) {
            return;
        }

        this.messageReceiveEvents.add(event);
    }

    /**
     * 添加事件。
     *
     * @param event
     */
    public void addEvent(MessageSendEvent event) {
        this.addEvent(MessageSendEvent.NAME);

        if (this.messageSendEvents.contains(event)) {
            return;
        }

        this.messageSendEvents.add(event);
    }

    /**
     * 移除事件。
     *
     * @param event
     */
    public void removeEvent(MessageReceiveEvent event) {
        this.removeEvent(MessageReceiveEvent.NAME);

        this.messageReceiveEvents.remove(event);
    }

    /**
     * 移除事件。
     *
     * @param event
     */
    public void removeEvent(MessageSendEvent event) {
        this.removeEvent(MessageSendEvent.NAME);

        this.messageSendEvents.remove(event);
    }

    /**
     *
     * @param message
     * @return
     */
    public MessageReceiveEvent queryReceiveEvent(Message message) {
        long sourceId = message.getSource().longValue();
        for (MessageReceiveEvent event : this.messageReceiveEvents) {
            if (sourceId == 0 && null != event.getContact()) {
                if (message.getTo().longValue() == event.getContact().getId().longValue()) {
                    event.setMessage(message);
                    return event;
                }
            }
            else if (sourceId != 0 && null != event.getGroup()) {
                if (sourceId == event.getGroup().getId().longValue()) {
                    event.setMessage(message);
                    return event;
                }
            }
        }

        return null;
    }

    public MessageSendEvent querySendEvent(Message message) {
        long fromId = message.getFrom().longValue();
        for (MessageSendEvent event : this.messageSendEvents) {
            Contact contact = event.getContact();
            if (null != contact && contact.getId().longValue() == fromId) {
                event.setMessage(message);
                return event;
            }
        }

        return null;
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
