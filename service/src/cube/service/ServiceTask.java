/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.Task;
import cube.common.entity.Device;
import cube.service.auth.AuthService;
import org.json.JSONObject;

/**
 * 用于服务单元的异步任务。提供一些辅助方法。
 */
public abstract class ServiceTask extends Task {

    public final static long ONE_WEEK = 7L * 24 * 60 * 60 * 1000;

    public final static long ONE_MONTH = 30L * 24 * 60 * 60 * 1000;

    public final static long THREE_MONTHS = ONE_MONTH * 3;

    protected ResponseTime responseTime;

    public ServiceTask(Cellet cellet, TalkContext talkContext, Primitive primitive) {
        super(cellet, talkContext, primitive);
    }

    public ServiceTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive);
        this.responseTime = responseTime;
    }

    protected void markResponseTime() {
        if (null == this.responseTime) {
            return;
        }

        this.responseTime.ending = System.currentTimeMillis();
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

    protected JSONObject makePacketPayload(int stateCode, JSONObject data) {
        JSONObject payload = new JSONObject();
        payload.put("code", stateCode);
        payload.put("data", data);
        return payload;
    }

    /**
     * 生成应答负载数据的应答原语。
     *
     * @param action
     * @param request
     * @param packetPayload
     * @return
     */
    private ActionDialect makeDispatcherResponse(ActionDialect action, Packet request, JSONObject packetPayload) {
        Packet response = new Packet(request.sn, request.name, packetPayload);
        ActionDialect responseDialect = response.toDialect();
        Director.copyPerformer(action, responseDialect);
        return responseDialect;
    }

    /**
     * 生成应答负载数据的应答原语。
     *
     * @param action
     * @param request
     * @param packetName
     * @param packetPayload
     * @return
     */
    private ActionDialect makeDispatcherResponse(ActionDialect action, Packet request, String packetName, JSONObject packetPayload) {
        Packet response = new Packet(request.sn, packetName, packetPayload);
        ActionDialect responseDialect = response.toDialect();
        Director.copyPerformer(action, responseDialect);
        return responseDialect;
    }

    /**
     * 生成应答负载数据的应答原语。
     *
     * @param action
     * @param request
     * @param stateCode
     * @param data
     * @return
     */
    protected ActionDialect makeResponse(ActionDialect action, Packet request, int stateCode, JSONObject data) {
        JSONObject payload = this.makePacketPayload(stateCode, data);
        return this.makeDispatcherResponse(action, request, payload);
    }

    /**
     * 生成应答负载数据的应答原语。
     *
     * @param action
     * @param request
     * @param packetName
     * @param stateCode
     * @param data
     * @return
     */
    protected ActionDialect makeResponse(ActionDialect action, Packet request, String packetName, int stateCode, JSONObject data) {
        JSONObject payload = this.makePacketPayload(stateCode, data);
        return this.makeDispatcherResponse(action, request, packetName, payload);
    }

    /**
     * 生成异步应答数据的原语。
     *
     * @param request
     * @param stateCode
     * @param data
     * @param id
     * @param domain
     * @param device
     * @return
     */
    protected ActionDialect makeAsynResponse(Packet request, long id, String domain, Device device,
                                               int stateCode, JSONObject data) {
        JSONObject payload = this.makePacketPayload(stateCode, data);
        Packet response = new Packet(request.sn, request.name, payload);
        ActionDialect responseDialect = response.toDialect();
        Director.attachDirector(responseDialect, id, domain, device);
        return responseDialect;
    }
}
