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

package cube.service;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cube.auth.AuthToken;
import cube.common.Packet;
import cube.common.StateCode;
import cube.common.Task;
import cube.service.auth.AuthService;

/**
 * 用于服务单元的异步任务。提供一些辅助方法。
 */
public abstract class ServiceTask extends Task {

    public ServiceTask(Cellet cellet, TalkContext talkContext, Primitive primitive) {
        super(cellet, talkContext, primitive);
    }

    /**
     * 提取令牌。
     *
     * @param actionDialect
     * @return
     */
    protected AuthToken extractAuthToken(ActionDialect actionDialect) {
        if (actionDialect.containsParam("token")) {
            String tokenCode = actionDialect.getParamAsString("token");
            AuthService authService = (AuthService) this.kernel.getModule(AuthService.NAME);
            AuthToken token = authService.getToken(tokenCode);
            return token;
        }

        return null;
    }

    /**
     * 生成指定包名和负载数据的应答原语。
     * @param requestData
     * @param packetName
     * @param packetPayload
     * @return
     */
    protected ActionDialect makeResponse(ActionDialect requestData, String packetName, JSONObject packetPayload) {
        // 添加状态信息
        try {
            packetPayload.put(StateCode.KEY, StateCode.makeState(StateCode.OK, "OK"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        long sn = requestData.getParamAsLong("sn");
        Packet response = new Packet(sn, packetName, packetPayload);
        ActionDialect responseDialect = response.toDialect();
        Director.copyPerformer(requestData, responseDialect);
        return responseDialect;
    }

    /**
     * 生成指定状态码和描述的应答原语。
     * @param requestData
     * @param packetName
     * @param stateCode
     * @param stateDesc
     * @return
     */
    protected ActionDialect makeResponse(ActionDialect requestData, String packetName, int stateCode, String stateDesc) {
        JSONObject payload = new JSONObject();
        try {
            payload.put(StateCode.KEY, StateCode.makeState(stateCode, stateDesc));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        long sn = requestData.getParamAsLong("sn");
        Packet response = new Packet(sn, packetName, payload);
        ActionDialect responseDialect = response.toDialect();
        Director.copyPerformer(requestData, responseDialect);
        return responseDialect;
    }
}
