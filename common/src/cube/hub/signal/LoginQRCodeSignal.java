/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.signal;

import org.json.JSONObject;

/**
 * 客户端提交登录二维码信令。
 */
public class LoginQRCodeSignal extends Signal {

    public final static String NAME = "LoginQRCode";

    private long timestamp;

    public LoginQRCodeSignal(String channelCode) {
        super(NAME);
        this.setCode(channelCode);
        this.timestamp = System.currentTimeMillis();
    }

    public LoginQRCodeSignal(JSONObject json) {
        super(json);
        this.timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return this.timestamp;
    }
}
