/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common;

import org.json.JSONObject;

/**
 * 状态码。
 */
public final class StateCode {

    /**
     * 成功。
     */
    public final static int OK = 1000;

    /**
     * 数据请求错误。
     */
    public final static int BadRequest = 1400;

    /**
     * 未知的请求命令。
     */
    public final static int NotFound = 1404;

    /**
     * 没有找到授权码。
     */
    public final static int NoAuthToken = 1501;

    /**
     * 系统忙。
     */
    public final static int SystemBusy = 1502;

    /**
     * 请求服务超时。
     */
    public final static int ServiceTimeout = 2001;

    /**
     * 负载格式错误。
     */
    public final static int PayloadFormat = 2002;

    /**
     * 参数错误。
     */
    public final static int InvalidParameter = 2003;

    /**
     * 网关错误。
     */
    public final static int GatewayError = 2101;

    /**
     * 创建状态描述对象。
     *
     * @param stateCode 状态码。
     * @param stateDesc 状态描述。
     * @return 返回状态的 JSON 格式数据。
     */
    public static JSONObject makeState(int stateCode, String stateDesc) {
        JSONObject state = new JSONObject();
        state.put("code", stateCode);
        state.put("desc", stateDesc);
        return state;
    }
}
