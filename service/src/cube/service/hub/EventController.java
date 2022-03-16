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

package cube.service.hub;

import cube.common.entity.ClientDescription;
import cube.hub.Event;
import cube.hub.Product;
import cube.hub.event.LoginQRCodeEvent;
import cube.hub.event.NewMessageEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 事件控制器。
 */
public class EventController {

    private final static EventController instance = new EventController();

    /**
     * WeChat 实时队列。
     */
    private ConcurrentLinkedQueue<Event> weChatRTEventQueue;

    private ConcurrentHashMap<String, ConcurrentLinkedQueue<Event>> weChatEventMap;

    private EventController() {
        this.weChatRTEventQueue = new ConcurrentLinkedQueue<>();
        this.weChatEventMap = new ConcurrentHashMap<>();
    }

    public final static EventController getInstance() {
        return EventController.instance;
    }

    public void receive(Event event, ClientDescription clientDescription) {
        if (event.getProduct() == Product.WeChat) {
            if (NewMessageEvent.NAME.equals(event.getName())) {
                NewMessageEvent nme = (NewMessageEvent) event;

            }
            else if (LoginQRCodeEvent.NAME.equals(event.getName())) {
                this.weChatRTEventQueue.offer(event);
            }
        }
    }
}
