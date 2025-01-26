/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.applet;

import org.json.JSONObject;

/**
 * 微信小程序 Applet API 。
 */
public interface WeChatAppletAPI {

    JSONObject code2session(String jsCode);
}
