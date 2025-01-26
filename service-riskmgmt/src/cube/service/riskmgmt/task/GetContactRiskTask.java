/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.riskmgmt.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.auth.AuthService;
import cube.service.riskmgmt.RiskManagement;
import cube.service.riskmgmt.RiskManagementCellet;
import org.json.JSONObject;

/**
 * 获取联系人风险数据。
 */
public class GetContactRiskTask extends ServiceTask {

    public GetContactRiskTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        String tokenCode = getTokenCode(dialect);
        if (null == tokenCode || !packet.data.has("contactId")) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        RiskManagement service = ((RiskManagementCellet) this.cellet).getService();

        AuthService authService = (AuthService) service.getKernel().getModule(AuthService.NAME);
        AuthToken authToken = authService.getToken(tokenCode);
        if (null == authToken) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InconsistentToken.code, packet.data));
            markResponseTime();
            return;
        }

        long contactId = packet.data.getLong("contactId");

        int mask = service.getContactRiskMask(authToken.getDomain(), contactId);

        JSONObject data = new JSONObject();
        data.put("contactId", contactId);
        data.put("mask", mask);

        this.cellet.speak(this.talkContext,
                this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, data));
        markResponseTime();
    }
}
