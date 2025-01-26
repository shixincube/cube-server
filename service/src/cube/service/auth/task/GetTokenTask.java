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
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.state.AuthStateCode;
import cube.service.ServiceTask;
import cube.service.auth.AuthService;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 获取令牌任务。
 */
public class GetTokenTask extends ServiceTask {

    public GetTokenTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        JSONObject data = packet.data;
        String code = null;

        try {
            code = data.getString("code");
        } catch (JSONException e) {
            // 获取失败
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, AuthStateCode.InvalidParameter.code, data));
            markResponseTime();
            return;
        }

        AuthToken token = ((AuthService)this.kernel.getModule(AuthService.NAME)).getToken(code);
        if (null == token) {
            // 获取失败
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, AuthStateCode.Failure.code, data));
            markResponseTime();
            return;
        }

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, AuthStateCode.Ok.code, token.toJSON()));
        markResponseTime();
    }
}
