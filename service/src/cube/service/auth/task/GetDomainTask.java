/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.auth.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.AuthDomain;
import cube.common.state.AuthStateCode;
import cube.service.ServiceTask;
import cube.service.auth.AuthService;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 获取访问域信息任务。
 */
public class GetDomainTask extends ServiceTask {

    public GetDomainTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        JSONObject data = packet.data;
        String domain = null;

        try {
            domain = data.getString("domain");
        } catch (JSONException e) {
            // 获取失败
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, AuthStateCode.InvalidParameter.code, data));
            markResponseTime();
            return;
        }

        AuthDomain authDomain = ((AuthService) this.kernel.getModule(AuthService.NAME)).getAuthDomain(domain);
        if (null == authDomain) {
            // 获取失败
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, AuthStateCode.Failure.code, data));
            markResponseTime();
            return;
        }

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, AuthStateCode.Ok.code, authDomain.toJSON()));
        markResponseTime();
    }
}
