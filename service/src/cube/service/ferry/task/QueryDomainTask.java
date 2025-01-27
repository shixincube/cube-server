/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.ferry.task;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.AuthDomain;
import cube.ferry.DomainInfo;
import cube.ferry.DomainMember;
import cube.ferry.FerryStateCode;
import cube.service.ServiceTask;
import cube.service.ferry.FerryCellet;
import cube.service.ferry.FerryService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 查询域信息任务。
 */
public class QueryDomainTask extends ServiceTask {

    public QueryDomainTask(FerryCellet cellet, TalkContext talkContext, Primitive primitive,
                           ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        JSONObject data = packet.data;
        if (!data.has("domain")) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FerryStateCode.InvalidParameter.code, data));
            markResponseTime();
            return;
        }

        FerryService service = ((FerryCellet) this.cellet).getFerryService();
        String domain = data.getString("domain");

        AuthDomain authDomain = service.getAuthDomain(domain);
        if (null == authDomain) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FerryStateCode.InvalidDomain.code, data));
            markResponseTime();
            return;
        }

        DomainInfo domainInfo = service.getDomainInfo(domain);
        if (null == domainInfo) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, FerryStateCode.InvalidDomain.code, data));
            markResponseTime();
            return;
        }

        List<DomainMember> list = service.listDomainMember(domain);
        JSONArray memberArray = new JSONArray();
        for (DomainMember member : list) {
            memberArray.put(member.toJSON());
        }

        JSONObject response = new JSONObject();
        response.put("domain", authDomain.toJSON());
        response.put("info", domainInfo.toJSON());
        response.put("members", memberArray);

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, FerryStateCode.Ok.code, response));
        markResponseTime();
    }
}
