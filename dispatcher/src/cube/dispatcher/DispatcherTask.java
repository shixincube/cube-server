/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.StateCode;
import cube.common.Task;
import org.json.JSONObject;

/**
 * 调度机线程任务抽象。
 */
public abstract class DispatcherTask extends Task {

    protected Performer performer;

    public ResponseTime responseTime;

    private Packet request;

    private ActionDialect action;

    public DispatcherTask(Cellet cellet, TalkContext talkContext, Primitive primitive, Performer performer) {
        super(cellet, talkContext, primitive);
        this.performer = performer;

        if (null != primitive) {
            this.action = DialectFactory.getInstance().createActionDialect(this.primitive);
            this.request = new Packet(this.action);
        }
    }

    public void reset(TalkContext talkContext, Primitive primitive) {
        this.talkContext = talkContext;
        this.primitive = primitive;

        if (null != primitive) {
            this.action = DialectFactory.getInstance().createActionDialect(this.primitive);
            this.request = new Packet(this.action);
        }
    }

    public ActionDialect getAction() {
        return this.action;
    }

    public Packet getRequest() {
        return this.request;
    }

    public void markResponseTime() {
        if (null != this.responseTime) {
            this.responseTime.ending = System.currentTimeMillis();
        }
    }

    protected ActionDialect makeResponse(ActionDialect response) {
        response.addParam("state", StateCode.makeState(StateCode.OK, "OK"));
        return response;
    }

    protected ActionDialect makeResponse(JSONObject payload, int stateCode, String desc) {
        Packet packet = new Packet(this.request.sn, this.request.name, payload);
        ActionDialect response = packet.toDialect();
        response.addParam("state", StateCode.makeState(stateCode, desc));
        return response;
    }

    protected ActionDialect makeGatewayErrorResponse(Packet response) {
        ActionDialect result = response.toDialect();
        result.addParam("state", StateCode.makeState(StateCode.GatewayError, "Gateway error"));
        return result;
    }

    /**
     * 打包为客户端的格式。
     *
     * @param data
     * @param code
     * @return
     */
    protected ActionDialect packResponse(JSONObject data, int code) {
        JSONObject payload = new JSONObject();
        payload.put("data", data);
        payload.put("code", code);
        Packet packet = new Packet(this.request.sn, this.request.name, payload);
        ActionDialect response = packet.toDialect();
        response.addParam("state", StateCode.makeState(StateCode.OK, "OK"));
        return response;
    }

    /**
     * 打包为客户端的格式。
     *
     * @param data
     * @param code
     * @param stateCode
     * @param stateDesc
     * @return
     */
    protected ActionDialect packResponse(JSONObject data, int code, int stateCode, String stateDesc) {
        JSONObject payload = new JSONObject();
        payload.put("data", data);
        payload.put("code", code);
        Packet packet = new Packet(this.request.sn, this.request.name, payload);
        ActionDialect response = packet.toDialect();
        response.addParam("state", StateCode.makeState(stateCode, stateDesc));
        return response;
    }

    /**
     * 标记状态码。
     *
     * @param response
     * @return
     */
    public static ActionDialect appendState(ActionDialect response) {
        response.addParam("state", StateCode.makeState(StateCode.OK, "OK"));
        return response;
    }
}
