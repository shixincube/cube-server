/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.event;

import org.json.JSONObject;

import java.io.File;

/**
 * 登录二维码事件。
 */
public class LoginQRCodeEvent extends WeChatEvent {

    public final static String NAME = "LoginQRCode";

    private Long pretenderId;

    public LoginQRCodeEvent(long sn, String channelCode, File file) {
        super(sn, NAME, file);
        setCode(channelCode);
    }

    public LoginQRCodeEvent(JSONObject json) {
        super(json);
    }

    public void setPretenderId(Long pretenderId) {
        this.pretenderId = pretenderId;
    }

    @Override
    public Long getPretenderId() {
        return this.pretenderId;
    }
}
