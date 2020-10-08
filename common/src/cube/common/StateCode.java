/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.common;

import cell.util.json.JSONException;
import cell.util.json.JSONObject;

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
     * 授权码错误。
     */
    public final static int NoAuthToken = 1501;

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
     * 状态码的主键。
     */
    public final static String KEY = "_state_";

    /**
     * 提取状态码。
     * @param payload
     * @return
     */
    public static int extractCode(JSONObject payload) {
        int code = -1;

        try {
            if (payload.has(KEY)) {
                JSONObject state = payload.getJSONObject(KEY);
                code = state.getInt("code");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return code;
    }

    /**
     * 提取状态描述。
     * @param payload
     * @return
     */
    public static String extractDesc(JSONObject payload) {
        String desc = "";

        try {
            if (payload.has(KEY)) {
                JSONObject state = payload.getJSONObject(KEY);
                desc = state.getString("desc");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return desc;
    }

    /**
     * 创建状态描述对象。
     * @param stateCode
     * @param stateDesc
     * @return
     */
    public static JSONObject makeState(int stateCode, String stateDesc) {
        JSONObject state = new JSONObject();
        try {
            state.put("code", stateCode);
            state.put("desc", stateDesc);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return state;
    }
}
