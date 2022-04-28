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

package cube.hub;

import cube.hub.event.*;
import org.json.JSONObject;

/**
 * 事件构建器。
 */
public class EventBuilder {

    private EventBuilder() {
    }

    public static Event build(JSONObject eventJson) {
        Product product = Product.parse(eventJson.getString("product"));
        if (product == Product.WeChat) {
            return buildWeChatEvent(eventJson);
        }

        return null;
    }

    private static Event buildWeChatEvent(JSONObject eventJson) {
        String name = eventJson.getString("name");

        Event event = null;

        if (RollPollingEvent.NAME.equals(name)) {
            event = new RollPollingEvent(eventJson);
        }
        else if (SubmitMessagesEvent.NAME.equals(name)) {
            event = new SubmitMessagesEvent(eventJson);
        }
        else if (ContactDataEvent.NAME.equals(name)) {
            event = new ContactDataEvent(eventJson);
        }
        else if (MessagesEvent.NAME.equals(name)) {
            event = new MessagesEvent(eventJson);
        }
        else if (AccountEvent.NAME.equals(name)) {
            event = new AccountEvent(eventJson);
        }
        else if (ConversationsEvent.NAME.equals(name)) {
            event = new ConversationsEvent(eventJson);
        }
        else if (ReportEvent.NAME.equals(name)) {
            event = new ReportEvent(eventJson);
        }
        else if (ContactZoneEvent.NAME.equals(name)) {
            event = new ContactZoneEvent(eventJson);
        }
        else if (GroupDataEvent.NAME.equals(name)) {
            event = new GroupDataEvent(eventJson);
        }
        else if (FileLabelEvent.NAME.equals(name)) {
            event = new FileLabelEvent(eventJson);
        }
        else if (SendMessageEvent.NAME.equals(name)) {
            event = new SendMessageEvent(eventJson);
        }
        else if (LoginQRCodeEvent.NAME.equals(name)) {
            event = new LoginQRCodeEvent(eventJson);
        }
        else if (AllocatedEvent.NAME.equals(name)) {
            event = new AllocatedEvent(eventJson);
        }
        else if (LogoutEvent.NAME.equals(name)) {
            event = new LogoutEvent(eventJson);
        }
        else if (AddFriendEvent.NAME.equals(name)) {
            event = new AddFriendEvent(eventJson);
        }
        else if (AlarmEvent.NAME.equals(name)) {
            event = new AlarmEvent(eventJson);
        }
        else if (ScreenshotEvent.NAME.equals(name)) {
            event = new ScreenshotEvent(eventJson);
        }
        else if (AckEvent.NAME.equals(name)) {
            event = new AckEvent(eventJson);
        }

        return event;
    }
}
