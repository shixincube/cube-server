/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.service.aigc.module;

import cell.util.Utils;
import cube.aigc.Sentiment;
import cube.common.entity.*;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.ChatListener;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 互动舞台。
 */
public class Stage extends Entity {

    private List<String> words;

    public boolean inference = false;

    public List<ChartResource> chartResources;

    public List<AttachmentResource> attachmentResources;

    public Stage(List<String> words) {
        super(Utils.generateSerialNumber());
        this.words = words;
        this.chartResources = new ArrayList<>();
        this.attachmentResources = new ArrayList<>();
    }

    public boolean isComplex() {
        return !this.chartResources.isEmpty() || !this.attachmentResources.isEmpty();
    }

    public void perform(AIGCService service, AIGCChannel channel, StageListener listener) {
        Module mod = ModuleManager.getInstance().matchModule(this.words);
        if (null == mod) {
            return;
        }

        if (mod instanceof PublicOpinion) {
            PublicOpinion publicOpinion = (PublicOpinion) mod;

            if (!this.chartResources.isEmpty()) {
                ChartResource chartResource = this.chartResources.get(0);
                // 获取时间线
                ChartSeries.Timeline timeline = chartResource.chartSeries.timeline;
                ChartSeries.TimePoint starting = timeline.first();
                ChartSeries.TimePoint ending = timeline.last();

                List<AIGCChatRecord> result = new ArrayList<>();
                AtomicInteger total = new AtomicInteger(0);
                AtomicInteger callCount = new AtomicInteger(0);

                List<String> negativeQueries = publicOpinion.makeEvaluatingArticleQueries(chartResource.chartSeries.label,
                        Sentiment.Negative, starting.year, starting.month, starting.date, ending.date);
                if (!negativeQueries.isEmpty()) {
                    total.addAndGet(negativeQueries.size());

                    for (String query : negativeQueries) {
                        service.singleChat(channel,
                                service.selectUnitBySubtask(AICapability.NaturalLanguageProcessing.Conversational),
                                query, new ChatListener() {
                                    @Override
                                    public void onChat(AIGCChannel channel, AIGCChatRecord record) {
                                        result.add(record);
                                        callback(total.get(), callCount, result, listener);
                                    }

                                    @Override
                                    public void onFailed(AIGCChannel channel) {
                                        callback(total.get(), callCount, result, listener);
                                    }
                                });
                    }
                }

                List<String> positiveQueries = publicOpinion.makeEvaluatingArticleQueries(chartResource.chartSeries.label,
                        Sentiment.Positive, starting.year, starting.month, starting.date, ending.date);
                if (!positiveQueries.isEmpty()) {
                    total.addAndGet(positiveQueries.size());

                    for (String query : positiveQueries) {
                        service.singleChat(channel,
                                service.selectUnitBySubtask(AICapability.NaturalLanguageProcessing.Conversational),
                                query, new ChatListener() {
                                    @Override
                                    public void onChat(AIGCChannel channel, AIGCChatRecord record) {
                                        result.add(record);
                                        callback(total.get(), callCount, result, listener);
                                    }

                                    @Override
                                    public void onFailed(AIGCChannel channel) {
                                        callback(total.get(), callCount, result, listener);
                                    }
                                });
                    }
                }
            }
        }
    }

    private void callback(int targetTotal, AtomicInteger callCount,
                          List<AIGCChatRecord> result, StageListener listener) {
        // 更新计数
        int count = callCount.incrementAndGet();
        if (targetTotal == count) {
            // 结束
            StringBuilder buf = new StringBuilder();
            for (AIGCChatRecord record : result) {

            }
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
