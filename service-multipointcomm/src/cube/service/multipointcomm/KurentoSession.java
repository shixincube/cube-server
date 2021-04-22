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
import cube.service.multipointcomm.signaling.CandidateSignaling;
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

    private final WebRtcEndpoint outgoingMedia;

    private final ConcurrentMap<Long, WebRtcEndpoint> incomingMedia;

    public KurentoSession(Portal portal, CommField commField, CommFieldEndpoint endpoint, MediaPipeline pipeline) {
        this.portal = portal;
        this.commField = commField;
        this.commFieldEndpoint = endpoint;

        this.outgoingMedia = new WebRtcEndpoint.Builder(pipeline).build();
        this.outgoingMedia.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {
            @Override
            public void onEvent(IceCandidateFoundEvent event) {
                IceCandidate iceCandidate = event.getCandidate();
                CandidateSignaling signaling = new CandidateSignaling(commField,
                        endpoint.getContact(), endpoint.getDevice(), 0L);
                portal.emit(endpoint, signaling);
            }
        });

        this.incomingMedia = new ConcurrentHashMap<>();
    }

    public String getName() {
        return this.commFieldEndpoint.getName();
    }

    public Long getId() {
        return this.commFieldEndpoint.getId();
    }

    public void receiveFrom(KurentoSession session) {
        Logger.i(this.getClass(), "Session \"" + this.getName() + "\" : connecting with \""
                + session.getName() + "\" in field " + this.commField.getName());

        getEndpointForSession(session);
//        final String sdpAnswer = getEndpointForSession(session);
    }

    public CommFieldEndpoint getCommFieldEndpoint() {
        return this.commFieldEndpoint;
    }

    private WebRtcEndpoint getEndpointForSession(KurentoSession session) {
        if (session.getId().equals(this.getId())) {
            Logger.d(this.getClass(), "Endpoint \"" + session.getName() + "\" configuring loopback");
            return this.outgoingMedia;
        }

        Logger.d(this.getClass(), "Endpoint \"" + this.getName() + "\" : receiving from \"" + session.getName() + "\"");

        return null;
    }
}
