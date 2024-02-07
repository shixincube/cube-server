/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
