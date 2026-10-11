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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventCenter {

    private final static EventCenter sInstance = new EventCenter();

    private final Map<String, EventListener> listenerMap;

    private final Map<String, UnitMeta> unitMetaMap;

    // 保留数量与 Python Unit 一致，每个单元保留最近 20 个已结束流的路由。
    private static final int TERMINAL_STREAM_RETENTION = 20;

    // 空值用于记录早于启动应答到达的 streamStopped 事件。
    private final Map<String, UnitMeta> stoppedUnitMetaMap;

    private static final int PENDING_STOP_RETENTION = 1024;

    private EventCenter() {
        this.listenerMap = new ConcurrentHashMap<>();
        this.unitMetaMap = new ConcurrentHashMap<>();
        this.stoppedUnitMetaMap = new LinkedHashMap<>();
    }

    public static EventCenter getInstance() {
        return EventCenter.sInstance;
    }

    public void start(AIGCService service) {
        MultimodalHandler multimodalHandler = new MultimodalHandler(service);
        this.addListener(Events.StreamStarted, multimodalHandler);
        this.addListener(Events.StreamStopped, multimodalHandler);
        this.addListener(Events.Segment, multimodalHandler);
    }

    public synchronized void stop() {
        this.removeListener(Events.StreamStarted);
        this.removeListener(Events.StreamStopped);
        this.removeListener(Events.Segment);
        this.unitMetaMap.clear();
        this.stoppedUnitMetaMap.clear();
    }

    public synchronized void putUnitMeta(String id, UnitMeta unitMeta) {
        if (this.stoppedUnitMetaMap.containsKey(id)) {
            if (null == this.stoppedUnitMetaMap.get(id)) {
                this.stoppedUnitMetaMap.put(id, unitMeta);
                this.trimStoppedUnitMetas(unitMeta.unit);
            }
        }
        else {
            // 控制应答不能覆盖启动任务的频道和用量归属。
            this.unitMetaMap.putIfAbsent(id, unitMeta);
        }
    }

    public synchronized void removeUnitMeta(String id) {
        this.unitMetaMap.remove(id);
        this.stoppedUnitMetaMap.remove(id);
    }

    public synchronized void removeUnitMeta(AIGCUnit unit) {
        for (Map.Entry<String, UnitMeta> item : this.unitMetaMap.entrySet()) {
            if (item.getValue().unit.getQueryKey().equalsIgnoreCase(unit.getQueryKey())) {
                this.unitMetaMap.remove(item.getKey());
            }
        }
        Iterator<UnitMeta> iter = this.stoppedUnitMetaMap.values().iterator();
        while (iter.hasNext()) {
            UnitMeta item = iter.next();
            if (null != item && item.unit.getQueryKey().equalsIgnoreCase(unit.getQueryKey())) {
                iter.remove();
            }
        }
    }

    public synchronized UnitMeta getUnitMeta(String id) {
        UnitMeta meta = this.unitMetaMap.get(id);
        return (null != meta) ? meta : this.stoppedUnitMetaMap.get(id);
    }

    public synchronized MultimodalUnitMeta searchMultimodalUnitMeta(AIGCUnit unit) {
        for (UnitMeta item : this.unitMetaMap.values()) {
            if (item.unit.getQueryKey().equalsIgnoreCase(unit.getQueryKey())) {
                return (item instanceof MultimodalUnitMeta) ? (MultimodalUnitMeta) item : null;
            }
        }

        return null;
    }

    public synchronized void markStreamStopped(String id) {
        UnitMeta meta = this.unitMetaMap.remove(id);
        if (!this.stoppedUnitMetaMap.containsKey(id)) {
            this.stoppedUnitMetaMap.put(id, meta);
        }
        if (null != meta) {
            this.trimStoppedUnitMetas(meta.unit);
        }
        else {
            // 尚未收到启动应答的停止标记也须有界。
            int pending = 0;
            for (UnitMeta item : this.stoppedUnitMetaMap.values()) {
                if (null == item) {
                    ++pending;
                }
            }
            Iterator<UnitMeta> iter = this.stoppedUnitMetaMap.values().iterator();
            while (pending > PENDING_STOP_RETENTION && iter.hasNext()) {
                if (null == iter.next()) {
                    iter.remove();
                    --pending;
                }
            }
        }
    }

    private void trimStoppedUnitMetas(AIGCUnit unit) {
        int count = 0;
        for (UnitMeta item : this.stoppedUnitMetaMap.values()) {
            if (null != item && item.unit.getQueryKey().equalsIgnoreCase(unit.getQueryKey())) {
                ++count;
            }
        }
        Iterator<UnitMeta> iter = this.stoppedUnitMetaMap.values().iterator();
        while (count > TERMINAL_STREAM_RETENTION && iter.hasNext()) {
            UnitMeta item = iter.next();
            if (null != item && item.unit.getQueryKey().equalsIgnoreCase(unit.getQueryKey())) {
                iter.remove();
                --count;
            }
        }
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
