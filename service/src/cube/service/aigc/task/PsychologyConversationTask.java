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

package cube.service.aigc.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.aigc.psychology.composition.ReportRelation;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.AIGCChannel;
import cube.common.entity.AIGCUnit;
import cube.common.entity.GenerativeOption;
import cube.common.entity.GenerativeRecord;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.GenerateTextListener;
import cube.service.aigc.scene.PsychologyScene;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 心理学相关的对话任务。
 */
public class PsychologyConversationTask extends ServiceTask {

    public PsychologyConversationTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        String token = getTokenCode(dialect);
        if (null == token) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.NoToken.code, new JSONObject()));
            markResponseTime();
            return;
        }

        String channelCode = null;
        List<ReportRelation> reportSnList = null;
        String query = null;
        try {
            channelCode = packet.data.getString("channelCode");
            JSONArray array = packet.data.getJSONArray("relevance");
            reportSnList = new ArrayList<>();
            for (int i = 0; i < array.length(); ++i) {
                reportSnList.add(new ReportRelation(array.getJSONObject(i)));
            }
            query = packet.data.getString("query");
        } catch (Exception e) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();

        // 获取频道
        AIGCChannel channel = service.getChannel(channelCode);
        if (null == channel) {
            channel = service.createChannel(token, channelCode, channelCode);
        }

        // 获取单元
        AIGCUnit unit = service.selectUnitByName(PsychologyScene.getInstance().getUnitName());
        if (null == unit) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.UnitError.code, new JSONObject()));
            markResponseTime();
            return;
        }

        int maxHistories = 5;
        List<GenerativeRecord> histories = null;
        List<GenerativeRecord> attachments = null;

        ReportRelation relevance = reportSnList.get(0);
        if (channel.getHistories().isEmpty()) {
            GenerativeRecord addition = PsychologyScene.getInstance().buildAddition(relevance, false);
            if (null == addition) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.NoData.code, new JSONObject()));
                markResponseTime();
                return;
            }

            attachments = Collections.singletonList(addition);
        }
        else {
            // 非空历史
            histories = new ArrayList<>();
            GenerativeRecord trick = PsychologyScene.getInstance().buildHistory(relevance, false);
            if (null == trick) {
                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.NoData.code, new JSONObject()));
                markResponseTime();
                return;
            }

            histories.add(trick);

            for (GenerativeRecord history : channel.getHistories()) {
                histories.add(history);
                if (histories.size() >= maxHistories) {
                    break;
                }
            }
        }

        // 使用指定模型生成结果
        service.generateText(channel, unit, query, query, new GenerativeOption(), histories, 0,
                attachments, null, false, true, new GenerateTextListener() {
                    @Override
                    public void onGenerated(AIGCChannel channel, GenerativeRecord record) {
                        if (null != record) {
                            cellet.speak(talkContext,
                                    makeResponse(dialect, packet, AIGCStateCode.Ok.code,
                                            record.toCompactJSON()));
                        }
                        else {
                            cellet.speak(talkContext,
                                    makeResponse(dialect, packet, AIGCStateCode.Failure.code, packet.data));
                        }
                        markResponseTime();
                    }

                    @Override
                    public void onFailed(AIGCChannel channel, AIGCStateCode stateCode) {
                        cellet.speak(talkContext,
                                makeResponse(dialect, packet, AIGCStateCode.Failure.code, packet.data));
                        markResponseTime();
                    }
                });
    }
}
