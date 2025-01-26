/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
