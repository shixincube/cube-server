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

import cell.util.log.Logger;
import cube.common.entity.ClientDescription;
import cube.common.entity.Contact;
import cube.hub.Product;
import cube.hub.event.AllocatedEvent;
import cube.hub.event.Event;
import cube.hub.event.ReportEvent;
import cube.hub.event.SubmitMessagesEvent;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件控制器。
 */
public class EventController {

    /**
     * WeChat 事件对应映射。
     */
    private ConcurrentHashMap<Long, List<Event>> weChatEventMap;

    public EventController() {
        this.weChatEventMap = new ConcurrentHashMap<>();
    }

    public void receive(Event event) {
        if (event.getProduct() == Product.WeChat) {
            // WeChat 事件
            ClientDescription clientDescription = event.getDescription();

            if (SubmitMessagesEvent.NAME.equals(event.getName())) {
                SubmitMessagesEvent submitMessagesEvent = (SubmitMessagesEvent) event;

                if (Logger.isDebugLevel()) {
                    Logger.d(this.getClass(), "Event [" + submitMessagesEvent.getName() + "]"
                        + " - " + clientDescription.toString());
                }

                // 记录消息
            }
            else if (ReportEvent.NAME.equals(event.getName())) {
                WeChatHub.getInstance().updateReport((ReportEvent) event);
            }
            else if (AllocatedEvent.NAME.equals(event.getName())) {
                AllocatedEvent allocatedEvent = (AllocatedEvent) event;
                // 已分配账号
                WeChatHub.getInstance().reportAlloc(allocatedEvent.getPretenderId(),
                        allocatedEvent.getCode(), allocatedEvent.getAccount());
            }
            else {
                Logger.d(this.getClass(), "#receive - Ignored : " + event.getName());
            }
        }
    }
}