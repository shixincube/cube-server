/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm;

import cube.common.entity.CommField;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.service.multipointcomm.signaling.ByeSignaling;

/**
 * 信令工具。
 */
public final class SignalingTool {

    private SignalingTool() {
    }

    public static ByeSignaling createByeSignaling(Contact contact, Device device, CommField commField) {
        ByeSignaling signaling = new ByeSignaling(commField, contact, device);
        return signaling;
    }
}
