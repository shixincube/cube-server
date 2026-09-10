/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.event;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;

import java.util.HashMap;
import java.util.Map;

public class EventCenter {

    private final static EventCenter sInstance = new EventCenter();

    private Map<String, EventListener> listenerMap;

    private EventCenter() {
        this.listenerMap = new HashMap<>();
        this.addListener(Events.OmniVLSegment, new MultimodalHandler());
    }

    public static EventCenter getInstance() {
        return EventCenter.sInstance;
    }

    public void addListener(String name, EventListener listener) {
        this.listenerMap.put(name, listener);
    }

    public void removeListener(String name) {
        this.listenerMap.remove(name);
    }

    public void notifyEvent(ActionDialect dialect) {
        try {
            Event event = new Event(dialect.getParamAsJson("data"));
            EventListener listener = this.listenerMap.get(event.name);
            if (null != listener) {
                listener.onEvent(event);
            }
        } catch (Exception e) {
            Logger.e(this.getClass(), "#notifyEvent", e);
        }
    }
}
