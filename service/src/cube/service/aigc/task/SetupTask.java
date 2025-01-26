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
import cube.common.entity.AICapability;
import cube.common.entity.Contact;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * AIGC 单元配置。
 */
public class SetupTask extends ServiceTask {

    public SetupTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        Contact contact = new Contact(packet.data.getJSONObject("contact"));

        List<AICapability> capabilities = new ArrayList<>();
        JSONArray array = packet.data.getJSONArray("capabilities");
        for (int i = 0; i < array.length(); ++i) {
            JSONObject capabilityJson = array.getJSONObject(i);
            AICapability capability = new AICapability(capabilityJson);
            capabilities.add(capability);
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();

        List<AIGCUnit> units = service.setupUnit(contact, capabilities, this.talkContext);
        Logger.i(SetupTask.class, "AIGC unit " + contact.getName() + " setup : " + units.size());

        JSONArray unitArray = new JSONArray();
        for (AIGCUnit unit : units) {
            unitArray.put(unit.toJSON());
        }
        JSONObject responseData = new JSONObject();
        responseData.put("units", unitArray);

        this.cellet.speak(this.talkContext,
                this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, responseData));
        markResponseTime();
    }
}
