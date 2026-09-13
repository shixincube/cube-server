/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.event;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.common.entity.AIGCUnit;
import cube.service.aigc.AIGCService;
import cube.service.aigc.unit.MultimodalUnitMeta;

import java.util.HashMap;
import java.util.Map;

public class EventCenter {

    private final static EventCenter sInstance = new EventCenter();

    private final Map<String, EventListener> listenerMap;

    private final Map<String, MultimodalUnitMeta> multimodalUnitMetaMap;

    private EventCenter() {
        this.listenerMap = new HashMap<>();
        this.multimodalUnitMetaMap = new HashMap<>();
    }

    public static EventCenter getInstance() {
        return EventCenter.sInstance;
    }

    public void start(AIGCService service) {
        this.addListener(Events.OmniVLSegment, new MultimodalHandler(service));
    }

    public void stop() {
    }

    public void putUnitMeta(String id, MultimodalUnitMeta unitMeta) {
        this.multimodalUnitMetaMap.put(id, unitMeta);
    }

    public void removeUnitMeta(String id) {
        this.multimodalUnitMetaMap.remove(id);
    }

    public void removeUnitMeta(AIGCUnit unit) {
        for (Map.Entry<String, MultimodalUnitMeta> item : this.multimodalUnitMetaMap.entrySet()) {
            if (item.getValue().unit.getQueryKey().equalsIgnoreCase(unit.getQueryKey())) {
                this.multimodalUnitMetaMap.remove(item.getKey());
                break;
            }
        }
    }

    public MultimodalUnitMeta getUnitMeta(String id) {
        return this.multimodalUnitMetaMap.get(id);
    }

    public MultimodalUnitMeta searchUnitMeta(AIGCUnit unit) {
        for (MultimodalUnitMeta item : this.multimodalUnitMetaMap.values()) {
            if (item.unit.getQueryKey().equalsIgnoreCase(unit.getQueryKey())) {
                return item;
            }
        }

        return null;
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
                listener.onEvent(this, event);
            }
        } catch (Exception e) {
            Logger.e(this.getClass(), "#notifyEvent", e);
        }
    }
}
