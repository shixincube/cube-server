/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.contact;

import cube.common.entity.Contact;
import cube.common.entity.Device;

/**
 * 令牌设备描述。
 */
public class TokenDevice {
    protected Device device;

    protected Contact contact;

    public TokenDevice(Contact contact, Device device) {
        this.contact = contact;
        this.device = device;
    }
}
