/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm.signaling;

import org.json.JSONObject;
import org.kurento.client.IceCandidate;

/**
 * 信令操作辅助函数库。
 */
public final class Signalings {

    private Signalings() {
    }

    /**
     *
     * @param iceCandidate
     * @return
     */
    public static JSONObject toJSON(IceCandidate iceCandidate) {
        JSONObject json = new JSONObject();
        json.put("candidate", iceCandidate.getCandidate());
        json.put("sdpMid", iceCandidate.getSdpMid());
        json.put("sdpMLineIndex", iceCandidate.getSdpMLineIndex());
        return json;
    }
}
