/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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
import cube.common.entity.CommFieldEndpoint;
import cube.service.multipointcomm.signaling.AnswerSignaling;
import cube.service.multipointcomm.signaling.CandidateSignaling;
import cube.service.multipointcomm.signaling.Signaling;
import cube.service.multipointcomm.signaling.Signalings;
import org.json.JSONObject;
import org.kurento.client.*;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Kurento 单元的用户会话。
 */
public class KurentoSession implements Closeable {

    private final Portal portal;

    private final Long commFieldId;

    private final CommFieldEndpoint commFieldEndpoint;

    private final MediaPipeline pipeline;

    private final WebRtcEndpoint outgoingMedia;

    private final ConcurrentMap<Long, WebRtcEndpoint> incomingMedias;

    private CompositeSetting compositeSetting;

    public KurentoSession(Portal portal, Long commFieldId, CommFieldEndpoint endpoint, MediaPipeline pipeline) {
        this.portal = portal;
        this.commFieldId = commFieldId;
        this.commFieldEndpoint = endpoint;
        this.pipeline = pipeline;
        this.incomingMedias = new ConcurrentHashMap<>();

        this.outgoingMedia = new WebRtcEndpoint.Builder(pipeline).build();
        this.outgoingMedia.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {
            @Override
            public void onEvent(IceCandidateFoundEvent event) {
                IceCandidate iceCandidate = event.getCandidate();
                JSONObject candidate = Signalings.toJSON(iceCandidate);

                CandidateSignaling signaling = new CandidateSignaling(portal.getCommField(commFieldId), commFieldEndpoint);
                signaling.setCandidate(candidate);
                sendSignaling(signaling);
            }
        });
    }

    public String getName() {
        return this.commFieldEndpoint.getName();
    }

    public Long getId() {
        return this.commFieldEndpoint.getId();
    }

    public CommFieldEndpoint getCommFieldEndpoint() {
        return this.commFieldEndpoint;
    }

    protected WebRtcEndpoint getOutgoingWebRtcPeer() {
        return this.outgoingMedia;
    }

    public void activeComposite(Composite composite, CompositeType type) {
        if (null == this.compositeSetting) {
            this.compositeSetting = new CompositeSetting(type, composite);
        }
    }

    /**
     * 接收指定会话的音视频数据。
     *
     * @param session
     * @param sdpOffer
     */
    public void receiveFrom(KurentoSession session, String sdpOffer) {
        Logger.i(this.getClass(), "Session \"" + this.getName() + "\" : connecting with \""
                + session.getName() + "\" in field " + this.portal.getCommField(commFieldId).getName());

        // 生成应答的 SDP
        final String sdpAnswer = this.getEndpointForSession(session).processOffer(sdpOffer);

        Logger.d(this.getClass(), "Session \"" + this.getName() + "\" : Sdp Answer for \"" + session.getName() + "\"");

        // 将应答发送回对端
        AnswerSignaling answerSignaling = new AnswerSignaling(this.portal.getCommField(commFieldId), session.getCommFieldEndpoint());
        answerSignaling.setSDP(sdpAnswer);
        this.sendSignaling(answerSignaling);

        Logger.d(this.getClass(), "Gather Candidates : \"" + session.getName() + "\"");
        this.getEndpointForSession(session).gatherCandidates();
    }

    /**
     * 添加 ICE Candidate 到目标 Peer 。
     *
     * @param candidate
     * @param target
     */
    public void addCandidate(IceCandidate candidate, KurentoSession target) {
        if (target.getId().longValue() == this.getId().longValue()) {
            this.outgoingMedia.addIceCandidate(candidate);
        }
        else {
            WebRtcEndpoint peer = this.incomingMedias.get(target.getId());
            if (null != peer) {
                peer.addIceCandidate(candidate);
            }
        }
    }

    /**
     * 取消置顶会话上的数据接收连接。
     *
     * @param session
     */
    public void cancelFrom(KurentoSession session) {
        Logger.d(this.getClass(), "Session \"" + this.getName() + "\" : canceling reception from \"" + session.getName() + "\"");
        final WebRtcEndpoint incoming = this.incomingMedias.remove(session.getId());

        if (null == incoming) {
            return;
        }

        Logger.d(this.getClass(), "Session \"" + this.getName() + "\" : removing endpoint for \"" + session.getName() + "\"");
        incoming.release(new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                Logger.d(KurentoSession.class, "Session \"" + getName() +
                        "\" : Released successfully incoming EP for \"" + session.getName() + "\"");
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                Logger.w(KurentoSession.class, "Session \"" + getName() + "\" : Could not release incoming EP for \""
                        + session.getName() + "\"");
            }
        });
    }

    /**
     * 关闭当前会话的所有连接。
     */
    @Override
    public void close() throws IOException {
        Logger.d(this.getClass(), "Session \"" + this.getName() + "\" : Releasing resources");

        for (final Map.Entry<Long, WebRtcEndpoint> entry : this.incomingMedias.entrySet()) {
            final Long remoteId = entry.getKey();
            Logger.d(this.getClass(), "Session \"" + this.getName() + "\" : Releasing incoming EP for " + remoteId);

            final WebRtcEndpoint ep = entry.getValue();

            ep.release(new Continuation<Void>() {
                @Override
                public void onSuccess(Void result) throws Exception {
                    Logger.d(KurentoSession.class, "Session \"" + getName() + "\" : Released successfully incoming EP for " + remoteId);
                }

                @Override
                public void onError(Throwable cause) throws Exception {
                    Logger.w(KurentoSession.class, "Session \"" + getName() + "\" : Could not release incoming EP for " + remoteId);
                }
            });
        }

        this.outgoingMedia.release(new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                Logger.d(KurentoSession.class, "Session \"" + getName() + "\" : Released outgoing EP");
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                Logger.w(KurentoSession.class, "Session \"" + getName() + "\" : Could not release outgoing EP");
            }
        });
    }

    /**
     * 获取指定会话的 WebRTC 终端。
     *
     * @param session
     * @return
     */
    private WebRtcEndpoint getEndpointForSession(final KurentoSession session) {
        if (session.getId().equals(this.getId())) {
            Logger.d(this.getClass(), "Endpoint \"" + session.getName() + "\" configuring loopback");
            return this.outgoingMedia;
        }

        Logger.d(this.getClass(), "Endpoint \"" + this.getName() + "\" : receiving from \"" + session.getName() + "\"");

        WebRtcEndpoint incoming = this.incomingMedias.get(session.getId());

        if (null == incoming) {
            Logger.d(this.getClass(), "Endpoint \"" + this.getName() + "\" : creating new endpoint for \"" +
                    session.getName() + "\"");

            incoming = new WebRtcEndpoint.Builder(this.pipeline).build();
            incoming.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {
                @Override
                public void onEvent(IceCandidateFoundEvent event) {
                    CandidateSignaling signaling = new CandidateSignaling(portal.getCommField(commFieldId), session.getCommFieldEndpoint());
                    signaling.setCandidate(Signalings.toJSON(event.getCandidate()));
                    sendSignaling(signaling);
                }
            });

            this.incomingMedias.put(session.getId(), incoming);
        }

        Logger.d(getClass(), "Endpoint \"" + this.getName() + "\" obtained endpoint for \"" + session.getName() + "\"");

        if (null != this.compositeSetting) {
            this.compositeSetting.compositeInputHubPort.connect(incoming);
        }
        else {
            session.getOutgoingWebRtcPeer().connect(incoming);
        }

        return incoming;
    }

    protected void sendSignaling(Signaling signaling) {
        this.portal.emit(this.commFieldEndpoint, signaling);
    }


    /**
     * 混合流的数据设置。
     */
    protected class CompositeSetting {

        protected final CompositeType compositeType;

        protected HubPort compositeInputHubPort;

        private CompositeSetting(CompositeType compositeType, Composite composite) {
            this.compositeType = compositeType;

            MediaType mediaType = null;
            if (compositeType == CompositeType.Audio) {
                mediaType = MediaType.AUDIO;
            }
            else if (compositeType == CompositeType.Video) {
                mediaType = MediaType.VIDEO;
            }

            this.compositeInputHubPort = new HubPort.Builder(composite).build();
            if (null != mediaType) {
                this.compositeInputHubPort.connect(outgoingMedia, mediaType);
                outgoingMedia.connect(this.compositeInputHubPort, mediaType);
            }
            else {
                this.compositeInputHubPort.connect(outgoingMedia);
                outgoingMedia.connect(this.compositeInputHubPort);
            }
        }
    }
}
