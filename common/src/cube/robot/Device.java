/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.robot;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * Roboengine 的设备描述。
 */
public class Device implements JSONable {

    private JSONObject data;

    public Device(JSONObject data) {
        this.data = data;
    }

    /**
     * 获取设备描述的数据。
     *
     * @return 返回 JSON 形式的设备描述数据。
     */
    public JSONObject getData() {
        return this.data;
    }

    @Override
    public JSONObject toJSON() {
        return this.data;
    }

    @Override
    public JSONObject toCompactJSON() {
        return toJSON();
    }
}
