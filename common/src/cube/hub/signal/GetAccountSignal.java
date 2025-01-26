/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.signal;

import org.json.JSONObject;

/**
 * 获取账号信息。
 */
public class GetAccountSignal extends Signal {

    public final static String NAME = "GetAccount";

    public GetAccountSignal(String channelCode) {
        super(NAME);
        setCode(channelCode);
    }

    public GetAccountSignal(JSONObject json) {
        super(json);
    }
}
