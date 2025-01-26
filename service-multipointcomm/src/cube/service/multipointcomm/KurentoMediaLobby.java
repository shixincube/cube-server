/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm;

import cell.util.log.Logger;
import org.kurento.client.Composite;
import org.kurento.client.Continuation;
import org.kurento.client.HubPort;
import org.kurento.client.MediaPipeline;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 被管理的每一个媒体管道上的数据封装。
 */
public class KurentoMediaLobby implements MediaLobby {

    protected final Long commFieldId;

    protected final MediaPipeline pipeline;

    /**
     * Comm Field Endpoint 对应的 Session
     */
    private final ConcurrentMap<Long, KurentoSession> endpointSessionMap;

    protected Composite composite;

    protected HubPort compositeOutputHubPort;

    /**
     * 每个终端在接收混码流时，对应的 Incoming 会话
     */
    protected ConcurrentMap<Long, KurentoIncomingSession> incomingSessionMap;

    protected long timestamp;

    protected KurentoMediaLobby(Long commFieldId, MediaPipeline pipeline) {
        this.timestamp = System.currentTimeMillis();
        this.commFieldId = commFieldId;
        this.pipeline = pipeline;
        this.endpointSessionMap = new ConcurrentHashMap<>();
    }

    public KurentoSession getSession(Long id) {
        this.timestamp = System.currentTimeMillis();
        return this.endpointSessionMap.get(id);
    }

    public void addSession(Long id, KurentoSession session) {
        this.endpointSessionMap.put(id, session);
        this.timestamp = System.currentTimeMillis();
    }

    public KurentoSession removeSession(Long id) {
        this.timestamp = System.currentTimeMillis();
        return this.endpointSessionMap.remove(id);
    }

    @Override
    public Collection<? extends MediaSession> getSessions() {
        return this.endpointSessionMap.values();
    }

    public void enableComposite() {
        // 创建混合集线器
        this.composite = new Composite.Builder(this.pipeline).build();
        // 创建输出端口
        this.compositeOutputHubPort = new HubPort.Builder(this.composite).build();
        // 创建 Map
        this.incomingSessionMap = new ConcurrentHashMap<>();
    }

    public KurentoIncomingSession getIncomingSession(Long id) {
        return this.incomingSessionMap.get(id);
    }

    public void addIncomingSession(Long id, KurentoIncomingSession session) {
        this.incomingSessionMap.put(id, session);
    }

    public KurentoIncomingSession removeIncomingSession(Long id) {
        return this.incomingSessionMap.remove(id);
    }

    protected void closePipeline() {
        this.pipeline.release(new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                Logger.d(KurentoForwardingMediaUnit.class, "Released Pipeline : " + commFieldId);
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                Logger.d(KurentoForwardingMediaUnit.class, "Could not release Pipeline : " + commFieldId);
            }
        });
    }
}
