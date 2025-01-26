/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.client.task;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.common.action.ClientAction;
import cube.common.entity.AIGCChannel;
import cube.common.entity.AIGCUnit;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.client.ClientCellet;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * AIGC 获取 AIG 单元任务。
 */
public class AIGCGetServiceInfoTask extends ClientTask {

    public AIGCGetServiceInfoTask(ClientCellet cellet, TalkContext talkContext, ActionDialect actionDialect) {
        super(cellet, talkContext, actionDialect);
    }

    @Override
    public void run() {
        AIGCService service = this.getAIGCService();

        List<AIGCUnit> unitList = service.getAllUnits();
        List<AIGCChannel> channelList = service.getAllChannels();

        ActionDialect response = new ActionDialect(ClientAction.AIGCGetServiceInfo.name);
        copyNotifier(response);

        JSONArray unitArray = new JSONArray();
        JSONArray channelArray = new JSONArray();

        for (AIGCUnit unit : unitList) {
            unitArray.put(unit.toJSON());
        }
        for (AIGCChannel channel : channelList) {
            channelArray.put(channel.toInfo());
        }

        JSONObject data = new JSONObject();
        data.put("unitList", unitArray);
        data.put("channelList", channelArray);
        response.addParam("data", data);
        response.addParam("code", AIGCStateCode.Ok.code);

        this.cellet.speak(this.talkContext, response);
    }
}
