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
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.state.AuthStateCode;
import cube.service.ServiceTask;
import cube.service.auth.AuthService;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 申请令牌任务。
 */
public class ApplyTokenTask extends ServiceTask {

    public ApplyTokenTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        JSONObject data = packet.data;
        String domain = "";
        String appKey = "";
        long cid = 0;

        try {
            domain = data.getString("domain");
            appKey = data.getString("appKey");
            if (data.has("cid")) {
                cid = data.getLong("cid");
            }
        } catch (JSONException e) {
            Logger.w(this.getClass(), "#run", e);
            this.cellet.speak(this.talkContext, this.makeResponse(action, packet, AuthStateCode.InvalidParameter.code, data));
            markResponseTime();
            return;
        }

        AuthToken token = ((AuthService) this.kernel.getModule(AuthService.NAME)).applyToken(domain, appKey, cid);
        if (null == token) {
            // 授权失败
            this.cellet.speak(this.talkContext, this.makeResponse(action, packet, AuthStateCode.InvalidDomain.code, data));
            markResponseTime();
            return;
        }

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, AuthStateCode.Ok.code, token.toJSON()));
        markResponseTime();
    }
}
