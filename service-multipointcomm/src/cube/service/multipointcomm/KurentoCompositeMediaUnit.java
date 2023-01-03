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

package cube.service.multipointcomm;

import cell.util.log.Logger;
import cube.common.entity.CommField;
import cube.common.entity.CommFieldEndpoint;
import cube.common.entity.MediaConstraint;
import cube.common.state.MultipointCommStateCode;
import org.json.JSONObject;
import org.kurento.client.IceCandidate;
import org.kurento.client.KurentoClient;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

/**
 * Kurento 单元。
 */
public class KurentoCompositeMediaUnit extends AbstractCompositeMediaUnit {

    private final MultipointCommService service;

    private final Portal portal;

    private final String url;

    private final ExecutorService executor;

    private KurentoClient kurentoClient;

    /**
     * Comm Field 对应的 Media Lobby 。
     */
    private final ConcurrentMap<Long, KurentoMediaLobby> lobbyMap;

    /**
     * 构造函数。
     *
     * @param service
     * @param portal
     * @param url
     * @param executor
     */
    public KurentoCompositeMediaUnit(MultipointCommService service, Portal portal, String url, ExecutorService executor) {
        this.service = service;
        this.portal = portal;
        this.url = url;
        this.executor = executor;

        try {
            this.kurentoClient = KurentoClient.create(url);
        } catch (Throwable e) {
            Logger.e(this.getClass(), "", e);
        }

        this.lobbyMap = new ConcurrentHashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void preparePipeline(CommField commField, CommFieldEndpoint endpoint) {
        if (null == this.kurentoClient) {
            return;
        }

        Logger.i(this.getClass(), "Prepare media pipeline: \"" + commField.getName() + "\" - " + commField.getId());

        KurentoMediaLobby lobby = this.lobbyMap.get(commField.getId());
        if (null == lobby) {
            lobby = new KurentoMediaLobby(commField.getId(), this.kurentoClient.createMediaPipeline());

            // 启用 Composite
            Logger.d(this.getClass(), "Enable composite: \"" + commField.getName() + "\"");
            lobby.enableComposite();

            this.lobbyMap.put(commField.getId(), lobby);
        }

        KurentoSession session = lobby.getSession(endpoint.getId());
        if (null == session) {
            session = new KurentoSession(this.portal, commField.getId(), endpoint, lobby.pipeline);

            // 根据媒体约束决定混合的流
            MediaConstraint constraint = commField.getMediaConstraint();
            CompositeType type = CompositeType.Both;
            if (constraint.audioEnabled() && !constraint.videoEnabled()) {
                type = CompositeType.Audio;
            }
            else if (constraint.videoEnabled() && !constraint.audioEnabled()) {
                type = CompositeType.Video;
            }

            // 激活混码集线器
            session.activeComposite(lobby.composite, type);

            lobby.addSession(endpoint.getId(), session);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MultipointCommStateCode subscribe(Long sn, CommField commField, CommFieldEndpoint endpoint,
                                             String sdpOffer, MediaUnitCallback callback) {
        KurentoMediaLobby lobby = this.lobbyMap.get(commField.getId());
        if (null == lobby) {
            return MultipointCommStateCode.NoPipeline;
        }

        final KurentoSession session = lobby.getSession(endpoint.getId());
        if (null == session) {
            return MultipointCommStateCode.NoCommFieldEndpoint;
        }

        // 获取来自混码器输出端口上的会话
        KurentoIncomingSession incomingSession = lobby.getIncomingSession(endpoint.getId());
        if (null == incomingSession) {
            incomingSession = new KurentoIncomingSession(this.portal, sn, commField.getId(), endpoint,
                    lobby.pipeline, lobby.compositeOutputHubPort);
            lobby.addIncomingSession(endpoint.getId(), incomingSession);
        }

        final KurentoIncomingSession incoming = incomingSession;
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                incoming.receive(sdpOffer);
                callback.on(commField, endpoint);
            }
        });

        return MultipointCommStateCode.Ok;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MultipointCommStateCode subscribe(Long sn, CommField commField, CommFieldEndpoint endpoint,
                                             CommFieldEndpoint target, String offerSDP,
                                             MediaUnitCallback callback) {
        KurentoMediaLobby lobby = this.lobbyMap.get(commField.getId());
        if (null == lobby) {
            return MultipointCommStateCode.NoPipeline;
        }

        final KurentoSession session = lobby.getSession(endpoint.getId());
        if (null == session) {
            return MultipointCommStateCode.NoCommFieldEndpoint;
        }

        final KurentoSession sender = lobby.getSession(target.getId());
        if (null == sender) {
            return MultipointCommStateCode.NoCommFieldEndpoint;
        }

        // 接收数据
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                session.receiveFrom(sender, offerSDP, sn);
                callback.on(commField, endpoint);
            }
        });

        return MultipointCommStateCode.Ok;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MultipointCommStateCode unsubscribe(CommField commField, CommFieldEndpoint endpoint, CommFieldEndpoint target,
                                               MediaUnitCallback callback) {
        KurentoMediaLobby lobby = this.lobbyMap.get(commField.getId());
        if (null == lobby) {
            return MultipointCommStateCode.NoPipeline;
        }

        final KurentoSession session = lobby.getSession(endpoint.getId());
        if (null == session) {
            return MultipointCommStateCode.NoCommFieldEndpoint;
        }

        final KurentoSession sender = lobby.getSession(target.getId());
        if (null == sender) {
            // 没有找到目标会话
            return MultipointCommStateCode.Ok;
        }

        // 取消接收数据
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                session.cancelFrom(sender);
                callback.on(commField, endpoint);
            }
        });

        return MultipointCommStateCode.Ok;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MultipointCommStateCode addCandidate(CommField commField, CommFieldEndpoint endpoint,
                                                CommFieldEndpoint target, JSONObject candidateJson) {
        KurentoMediaLobby lobby = this.lobbyMap.get(commField.getId());
        if (null == lobby) {
            return MultipointCommStateCode.NoPipeline;
        }

        KurentoSession session = lobby.getSession(endpoint.getId());
        if (null == session) {
            return MultipointCommStateCode.NoCommFieldEndpoint;
        }

        if (null == target) {
            return MultipointCommStateCode.DataStructureError;
        }

        KurentoSession targetSession = lobby.getSession(target.getId());
        if (null == targetSession) {
            return MultipointCommStateCode.NoCommFieldEndpoint;
        }

        // 添加 Candidate
        IceCandidate candidate = new IceCandidate(candidateJson.getString("candidate"),
                candidateJson.getString("sdpMid"), candidateJson.getInt("sdpMLineIndex"));
        session.addCandidate(candidate, targetSession);

        return MultipointCommStateCode.Ok;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MultipointCommStateCode addCandidate(Long sn, CommField commField, CommFieldEndpoint endpoint,
                                                JSONObject candidateJson) {
        KurentoMediaLobby lobby = this.lobbyMap.get(commField.getId());
        if (null == lobby) {
            return MultipointCommStateCode.NoPipeline;
        }

        KurentoSession session = lobby.getSession(endpoint.getId());
        if (null == session) {
            return MultipointCommStateCode.NoCommFieldEndpoint;
        }

        KurentoIncomingSession incomingSession = lobby.getIncomingSession(endpoint.getId());
        if (null == incomingSession) {
            return MultipointCommStateCode.NoPeerEndpoint;
        }

        if (incomingSession.getSN().longValue() == sn.longValue()) {
            // 添加 Candidate
            IceCandidate candidate = new IceCandidate(candidateJson.getString("candidate"),
                    candidateJson.getString("sdpMid"), candidateJson.getInt("sdpMLineIndex"));
            incomingSession.addCandidate(candidate);

            return MultipointCommStateCode.Ok;
        }
        else {
            Logger.e(this.getClass(), "Can NOT find incoming session: " + endpoint.getName());

            return MultipointCommStateCode.NoPeerEndpoint;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MultipointCommStateCode removeEndpoint(CommField commField, CommFieldEndpoint endpoint) {
        KurentoMediaLobby lobby = this.lobbyMap.get(commField.getId());
        if (null == lobby) {
            return MultipointCommStateCode.NoPipeline;
        }

        // 删除会话
        KurentoSession session = lobby.removeSession(endpoint.getId());
        if (null == session) {
            return MultipointCommStateCode.NoCommFieldEndpoint;
        }

        KurentoIncomingSession incomingSession = lobby.removeIncomingSession(endpoint.getId());
        try {
            incomingSession.close();
        } catch (Exception e) {
            // Nothing
        }

        for (CommFieldEndpoint participant : commField.getEndpoints()) {
            if (participant.equals(endpoint)) {
                continue;
            }

            // 让其他参与者取消接收流
            KurentoSession participantSession = lobby.getSession(participant.getId());
            participantSession.cancelFrom(session);
        }

        try {
            session.close();
        } catch (IOException e) {
            Logger.e(this.getClass(), "", e);
        }

        // 删除终端
        commField.removeEndpoint(endpoint);

        if (commField.numEndpoints() == 0) {
            Logger.d(this.getClass(), "Comm field \"" + commField.getName() + "\" empty.");
            // 已经没有终端在场域里，删除场域
            this.lobbyMap.remove(commField.getId());
            lobby.closePipeline();
        }

        return MultipointCommStateCode.Ok;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MultipointCommStateCode release(CommField commField) {
        KurentoMediaLobby lobby = this.lobbyMap.remove(commField.getId());
        if (null == lobby) {
            return MultipointCommStateCode.NoPipeline;
        }

        // 关闭所有会话
        Iterator<? extends MediaSession> iter = lobby.getSessions().iterator();
        while (iter.hasNext()) {
            KurentoSession session = (KurentoSession) iter.next();
            try {
                session.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 关闭通道
        lobby.closePipeline();

        return MultipointCommStateCode.Ok;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        for (KurentoMediaLobby lobby : this.lobbyMap.values()) {
            lobby.closePipeline();
        }
        this.lobbyMap.clear();

        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (null != this.kurentoClient) {
            this.kurentoClient.destroy();
            this.kurentoClient = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends MediaLobby> getAllLobbies() {
        return this.lobbyMap.values();
    }

    @Override
    public void onTick(long now) {
        if (null == this.kurentoClient) {
            try {
                this.kurentoClient = KurentoClient.create(this.url);
            } catch (Throwable e) {
                Logger.w(this.getClass(), "", e);
            }
        }

        // 删除超时的管道
        Iterator<KurentoMediaLobby> iter = this.lobbyMap.values().iterator();
        while (iter.hasNext()) {
            KurentoMediaLobby lobby = iter.next();
            CommField commField = this.service.getCommField(lobby.commFieldId);
            if (null != commField) {
                if (commField.numEndpoints() == 0) {
//                    Logger.d(this.getClass(), "CommField \"" + commField.getName() + "\" : no endpoint");
                    Logger.i(this.getClass(), "Media pipeline closed : " + commField.getName());
                    lobby.closePipeline();
                    iter.remove();
                }
                else {
                    Logger.d(this.getClass(), "CommField \"" + commField.getName() + "\" : " + commField.numEndpoints() + " endpoints");
                }
            }
            else {
//                Logger.e(this.getClass(), "CommField : " + lobby.commFieldId + " removed");
                Logger.w(this.getClass(), "Media pipeline closed : " + lobby.commFieldId);
                lobby.closePipeline();
                iter.remove();
            }
        }
    }
}
