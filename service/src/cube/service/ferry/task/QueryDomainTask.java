/*
 * This source file is part of Cube.
 * <p>
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2020-2024 Ambrose Xu.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
