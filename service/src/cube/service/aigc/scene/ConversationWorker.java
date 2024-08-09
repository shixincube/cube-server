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

package cube.service.aigc.scene;

import cube.aigc.psychology.composition.ReportRelation;
import cube.common.entity.AIGCChannel;
import cube.common.entity.AIGCUnit;
import cube.common.entity.GenerativeOption;
import cube.common.entity.GenerativeRecord;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.GenerateTextListener;

import java.util.ArrayList;
import java.util.List;

public class ConversationWorker {

    private AIGCService service;

    public ConversationWorker(AIGCService service) {
        this.service = service;
    }

    public AIGCStateCode work(String token, String channelCode, List<ReportRelation> reportRelationList,
                              String query, GenerateTextListener listener) {
        // 获取频道
        AIGCChannel channel = service.getChannel(channelCode);
        if (null == channel) {
            channel = service.createChannel(token, channelCode, channelCode);
        }

        // 获取单元
        AIGCUnit unit = service.selectUnitByName(PsychologyScene.getInstance().getUnitName());
        if (null == unit) {
            return AIGCStateCode.UnitError;
        }

//        int maxHistories = 5;

        List<GenerativeRecord> histories = null;
        String prompt = PsychologyScene.getInstance().buildPrompt(reportRelationList, query);

        /** FIXME 2024-08-09 放弃使用历史记录方式
        if (channel.getHistories().isEmpty()) {
            prompt = PsychologyScene.getInstance().buildPrompt(reportRelationList, query);
            if (null == prompt) {
                return AIGCStateCode.NoData;
            }
        }
        else {
            // 非空历史
            histories = new ArrayList<>();
            GenerativeRecord trick = PsychologyScene.getInstance().buildHistory(reportRelationList, query);
            if (null == trick) {
                return AIGCStateCode.NoData;
            }

            histories.add(trick);

            for (GenerativeRecord history : channel.getHistories()) {
                histories.add(history);
                if (histories.size() >= maxHistories) {
                    break;
                }
            }
        }*/

        // 使用指定模型生成结果
        service.generateText(channel, unit, query, prompt, new GenerativeOption(), histories, 0,
                null, null, false, true, listener);

        return AIGCStateCode.Ok;
    }
}
