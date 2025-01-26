/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.AIGCUnit;
import cube.common.entity.Contact;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * AIGC 单元拆除。
 */
public class TeardownTask extends ServiceTask {

    public TeardownTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        Contact contact = new Contact(packet.data.getJSONObject("contact"));

        AIGCService service = ((AIGCCellet) this.cellet).getService();

        List<AIGCUnit> units = service.teardownUnit(contact);
        Logger.i(TeardownTask.class, "AIGC unit " + contact.getName() + " teardown : " + units.size());

        JSONArray array = new JSONArray();
        for (AIGCUnit unit : units) {
            array.put(unit.toJSON());
        }
        JSONObject responseData = new JSONObject();
        responseData.put("units", array);

        this.cellet.speak(this.talkContext,
                this.makeResponse(dialect, packet,
                        (units.isEmpty()) ? AIGCStateCode.Failure.code : AIGCStateCode.Ok.code,
                        responseData));
        markResponseTime();
    }
}
