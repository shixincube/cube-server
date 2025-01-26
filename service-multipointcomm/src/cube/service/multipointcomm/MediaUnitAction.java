/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm;

/**
 * 媒体单元动作。
 */
public enum MediaUnitAction {

    /**
     * 发送信令。
     */
    Signaling("signaling"),

    /**
     * 信令应答。
     */
    SignalingAck("signalingAck"),

    /**
     * 未知动作。
     */
    Unknown("")

    ;

    public final String name;

    MediaUnitAction(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
