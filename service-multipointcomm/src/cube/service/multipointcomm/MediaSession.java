/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm;

import org.kurento.client.WebRtcEndpoint;

import java.io.Closeable;

/**
 * 描述每个终端连接会话。
 */
public interface MediaSession extends Closeable {

    /**
     * 获取会话端的出站 Peer 。
     *
     * @return
     */
    public WebRtcEndpoint getOutgoingPeer();
}
