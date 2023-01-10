/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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
import cell.core.talk.PrimitiveOutputStream;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.common.UniqueKey;
import cube.common.action.ClientAction;
import cube.common.entity.ClientDescription;
import cube.common.entity.Entity;
import cube.common.entity.Message;
import cube.service.client.event.MessageReceiveEvent;
import cube.service.client.event.MessageSendEvent;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端管理实体。
 */
public class ServerClient extends Entity {

    private Cellet cellet;

    protected ClientDescription desc;

    protected TalkContext talkContext;

    protected List<String> events;

    // Key : 实体唯一键
    protected Map<String, MessageReceiveEvent> messageReceiveEvents;

    // Key : 实体唯一键
    protected Map<String, MessageSendEvent> messageSendEvents;

    public ServerClient(Long id, Cellet cellet, TalkContext talkContext, ClientDescription desc) {
        super(id);
        this.cellet = cellet;
        this.talkContext = talkContext;
        this.desc = desc;
        this.events = new ArrayList<>();
        this.messageReceiveEvents = new ConcurrentHashMap<>();
        this.messageSendEvents = new ConcurrentHashMap<>();
    }

    public void resetTalkContext(TalkContext talkContext) {
        this.talkContext = talkContext;
    }

    public TalkContext getTalkContext() {
        return this.talkContext;
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

    public void transmit(ActionDialect actionDialect) {
        if (null == this.talkContext) {
            return;
        }

        this.cellet.speak(this.talkContext, actionDialect);
    }

    public void transmitStream(String streamName, File file) {
        if (null == this.talkContext) {
            return;
        }

        PrimitiveOutputStream stream = this.cellet.speakStream(this.talkContext, streamName);
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(file);
            byte[] bytes = new byte[4096];
            int length = 0;
            while ((length = fis.read(bytes)) > 0) {
                stream.write(bytes, 0, length);
            }

            stream.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }

            try {
                stream.close();
            } catch (IOException e) {
            }
        }
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
