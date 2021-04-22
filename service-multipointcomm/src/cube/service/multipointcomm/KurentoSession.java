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
import cube.common.entity.CommField;
import cube.common.entity.CommFieldEndpoint;
import cube.service.multipointcomm.signaling.AnswerSignaling;
import cube.service.multipointcomm.signaling.CandidateSignaling;
import cube.service.multipointcomm.signaling.Signaling;
import org.kurento.client.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Kurento 单元的用户会话。
 */
public class KurentoSession {

    private final Portal portal;

    private final CommField commField;

    private final CommFieldEndpoint commFieldEndpoint;

    private final MediaPipeline pipeline;

    private final WebRtcEndpoint outgoingMedia;

    private final ConcurrentMap<Long, WebRtcEndpoint> incomingMedia;

    public KurentoSession(Portal portal, CommField commField, CommFieldEndpoint endpoint, MediaPipeline pipeline) {
        this.portal = portal;
        this.commField = commField;
        this.commFieldEndpoint = endpoint;
        this.pipeline = pipeline;
        this.incomingMedia = new ConcurrentHashMap<>();

        this.outgoingMedia = new WebRtcEndpoint.Builder(pipeline).build();
        this.outgoingMedia.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {
            @Override
            public void onEvent(IceCandidateFoundEvent event) {
                IceCandidate iceCandidate = event.getCandidate();

//                CandidateSignaling signaling = new CandidateSignaling(commField,
//                        endpoint.getContact(), endpoint.getDevice(), 0L);
//                portal.emit(endpoint, signaling);
            }
        });
    }

    public String getName() {
        return this.commFieldEndpoint.getName();
    }

    public Long getId() {
        return this.commFieldEndpoint.getId();
    }

    protected WebRtcEndpoint getOutgoingWebRtcPeer() {
        return this.outgoingMedia;
    }

    public void receiveFrom(KurentoSession session, String sdpOffer) {
        Logger.i(this.getClass(), "Session \"" + this.getName() + "\" : connecting with \""
                + session.getName() + "\" in field " + this.commField.getName());

        // 生成应答的 SDP
        final String sdpAnswer = this.getEndpointForSession(session).processOffer(sdpOffer);

        Logger.d(this.getClass(), "Session \"" + this.getName() + "\" : Sdp Answer for \"" + session.getName() + "\"");

        // 将应答发送回对端
        AnswerSignaling answerSignaling = new AnswerSignaling(this.commField, session.getCommFieldEndpoint());
        answerSignaling.setSDP(sdpAnswer);
        this.sendSignaling(answerSignaling);

        Logger.d(this.getClass(), "Gather Candidates : \"" + session.getName() + "\"");
//        this.getEndpointForSession(session).gatherCandidates();
    }

    public CommFieldEndpoint getCommFieldEndpoint() {
        return this.commFieldEndpoint;
    }

    private WebRtcEndpoint getEndpointForSession(final KurentoSession session) {
        if (session.getId().equals(this.getId())) {
            Logger.d(this.getClass(), "Endpoint \"" + session.getName() + "\" configuring loopback");
            return this.outgoingMedia;
        }

        Logger.d(this.getClass(), "Endpoint \"" + this.getName() + "\" : receiving from \"" + session.getName() + "\"");

        WebRtcEndpoint incoming = this.incomingMedia.get(session.getId());

        if (null == incoming) {
            Logger.d(this.getClass(), "Endpoint \"" + this.getName() + "\" : creating new endpoint for \"" +
                    session.getName() + "\"");

            incoming = new WebRtcEndpoint.Builder(this.pipeline).build();

            incoming.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {
                @Override
                public void onEvent(IceCandidateFoundEvent event) {
                    // TODO
                    CandidateSignaling signaling = new CandidateSignaling(commField, session.getCommFieldEndpoint());
//                    portal.emit(commFieldEndpoint, signaling);
                }
            });

            this.incomingMedia.put(session.getId(), incoming);
        }

        Logger.d(getClass(), "Endpoint \"" + this.getName() + "\" obtained endpoint for \"" + session.getName() + "\"");

        session.getOutgoingWebRtcPeer().connect(incoming);

        return incoming;
    }

    protected void sendSignaling(Signaling signaling) {
        this.portal.emit(this.commFieldEndpoint, signaling);
    }
}
