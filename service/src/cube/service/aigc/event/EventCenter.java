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
import cube.service.aigc.unit.UnitMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventCenter {

    private final static EventCenter sInstance = new EventCenter();

    private final Map<String, EventListener> listenerMap;

    private final Map<String, UnitMeta> unitMetaMap;

    private EventCenter() {
        this.listenerMap = new HashMap<>();
        this.unitMetaMap = new ConcurrentHashMap<>();
    }

    public static EventCenter getInstance() {
        return EventCenter.sInstance;
    }

    public void start(AIGCService service) {
        this.addListener(Events.OmniVLSegment, new MultimodalHandler(service));
    }

    public void stop() {
    }

    public void putUnitMeta(String id, UnitMeta unitMeta) {
        this.unitMetaMap.put(id, unitMeta);
    }

    public void removeUnitMeta(String id) {
        this.unitMetaMap.remove(id);
    }

    public void removeUnitMeta(AIGCUnit unit) {
        for (Map.Entry<String, UnitMeta> item : this.unitMetaMap.entrySet()) {
            if (item.getValue().unit.getQueryKey().equalsIgnoreCase(unit.getQueryKey())) {
                this.unitMetaMap.remove(item.getKey());
                break;
            }
        }
    }

    public UnitMeta getUnitMeta(String id) {
        return this.unitMetaMap.get(id);
    }

    public MultimodalUnitMeta searchMultimodalUnitMeta(AIGCUnit unit) {
        for (UnitMeta item : this.unitMetaMap.values()) {
            if (item.unit.getQueryKey().equalsIgnoreCase(unit.getQueryKey())) {
                return (item instanceof MultimodalUnitMeta) ? (MultimodalUnitMeta) item : null;
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
