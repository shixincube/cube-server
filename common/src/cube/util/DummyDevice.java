/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util;

import cell.core.talk.TalkContext;
import cube.common.entity.Device;

/**
 * 哑元设备。
 */
public class DummyDevice extends Device {

    public DummyDevice(TalkContext context) {
        super("Dummy", "Cube 3.0", context);
    }
}
