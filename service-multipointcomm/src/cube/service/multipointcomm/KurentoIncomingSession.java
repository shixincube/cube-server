/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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

/**
 *
 */
public class KurentoIncomingSession implements Closeable {

    private final Portal portal;

    private final Long sn;

    private final Long commFieldId;

    private final CommFieldEndpoint commFieldEndpoint;

    private final MediaPipeline pipeline;

    private final WebRtcEndpoint outgoingMedia;

    public KurentoIncomingSession(Portal portal, Long sn, Long commFieldId, CommFieldEndpoint commFieldEndpoint,
                                  MediaPipeline pipeline, HubPort outputHubPort) {
        this.portal = portal;
        this.sn = sn;
        this.commFieldId = commFieldId;
        this.commFieldEndpoint = commFieldEndpoint;
        this.pipeline = pipeline;

        this.outgoingMedia = new WebRtcEndpoint.Builder(pipeline).build();
        this.outgoingMedia.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {
            @Override
            public void onEvent(IceCandidateFoundEvent event) {
                IceCandidate iceCandidate = event.getCandidate();
                JSONObject candidate = Signalings.toJSON(iceCandidate);

                CandidateSignaling signaling = new CandidateSignaling(portal.getCommField(commFieldId), commFieldEndpoint);
                signaling.setSN(sn);
                signaling.setCandidate(candidate);
                sendSignaling(signaling);
            }
        });

        // 输出端口连接
        outputHubPort.connect(this.outgoingMedia);
    }

    public Long getSN() {
        return this.sn;
    }

    public void receive(String sdpOffer) {
        // 生成应答的 SDP
        final String sdpAnswer = this.outgoingMedia.processOffer(sdpOffer);

        Logger.d(this.getClass(), "Incoming session : Sdp Answer for \"" + this.commFieldEndpoint.getName() + "\"");

        // 将应答发送回对端
        AnswerSignaling answerSignaling = new AnswerSignaling(this.sn, this.portal.getCommField(commFieldId), this.commFieldEndpoint);
        answerSignaling.setSDP(sdpAnswer);
        this.sendSignaling(answerSignaling);

        Logger.d(this.getClass(), "Gather Candidates : \"" + this.commFieldEndpoint.getName() + "\"");
        this.outgoingMedia.gatherCandidates();
    }

    public void addCandidate(IceCandidate candidate) {
        this.outgoingMedia.addIceCandidate(candidate);
    }

    protected void sendSignaling(Signaling signaling) {
        this.portal.emit(this.commFieldEndpoint, signaling);
    }

    @Override
    public void close() throws IOException {
        this.outgoingMedia.release(new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                Logger.d(KurentoSession.class, "Incoming session \"" + commFieldEndpoint.getName() + "\" : Released outgoing EP");
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                Logger.w(KurentoSession.class, "Incoming session \"" + commFieldEndpoint.getName() + "\" : Could not release outgoing EP");
            }
        });
    }
}
