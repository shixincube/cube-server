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

package cube.service.aigc.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.ChartReaction;
import cube.common.entity.ChartSeries;
import cube.common.entity.SearchResult;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.resource.ResourceCenter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 图表数据操作任务。
 */
public class ChartDataTask extends ServiceTask {

    public ChartDataTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        AuthToken authToken = extractAuthToken(dialect);
        if (null == authToken) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();
        JSONObject requestData = packet.data;
        String action = requestData.getString("action");

        System.out.println("XJW:\n" + requestData.toString(4));

        try {
            if (action.equalsIgnoreCase("insertReaction")) {
                JSONObject json = requestData.getJSONObject("reaction");
                boolean overwrite = requestData.has("overwrite") && requestData.getBoolean("overwrite");
                ChartReaction reaction = new ChartReaction(json);
                if (service.getStorage().insertChartReaction(reaction, overwrite)) {
                    this.cellet.speak(this.talkContext,
                            this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, new JSONObject()));
                    markResponseTime();
                    return;
                }
            }
            else if (action.equalsIgnoreCase("deleteReaction")) {
                String primary = requestData.getString("primary");
                if (service.getStorage().deleteChartReaction(primary)) {
                    this.cellet.speak(this.talkContext,
                            this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, new JSONObject()));
                    markResponseTime();
                    return;
                }
            }
            else if (action.equalsIgnoreCase("insertSeries")) {
                JSONObject json = requestData.getJSONObject("series");
                boolean overwrite = requestData.has("overwrite") && requestData.getBoolean("overwrite");
                ChartSeries series = new ChartSeries(json);
                if (service.getStorage().insertChartSeries(series, overwrite)) {
                    this.cellet.speak(this.talkContext,
                            this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, new JSONObject()));
                    markResponseTime();
                    return;
                }
            }
            else if (action.equalsIgnoreCase("deleteSeries")) {
                String name = requestData.getString("name");
                if (service.getStorage().deleteChartSeries(name)) {
                    this.cellet.speak(this.talkContext,
                            this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, new JSONObject()));
                    markResponseTime();
                    return;
                }
            }
        } catch (Exception e) {
            Logger.e(this.getClass(), "#run", e);
        }

        this.cellet.speak(this.talkContext,
                this.makeResponse(dialect, packet, AIGCStateCode.Failure.code, new JSONObject()));
        markResponseTime();
    }
}
