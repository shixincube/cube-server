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
import cube.aigc.atom.Atom;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.ChartReaction;
import cube.common.entity.ChartSeries;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 图表数据操作任务。
 */
public class ChartDataTask extends ServiceTask {

    public final static String ACTION_GET_ATOMS = "getAtoms";

    public final static String ACTION_INSERT_ATOMS = "insertAtoms";

    public final static String ACTION_DELETE_ATOMS = "deleteAtoms";

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
            else if (ACTION_GET_ATOMS.equalsIgnoreCase(action)) {
                String label = requestData.getString("label");
                String year = requestData.has("year") ? requestData.getString("year") : null;
                String month = requestData.has("month") ? requestData.getString("month") : null;
                String date = requestData.has("date") ? requestData.getString("date") : null;
                List<Atom> list = service.getStorage().readAtoms(label, year, month, date);

                JSONArray array = new JSONArray();
                for (Atom atom : list) {
                    array.put(atom.toJSON());
                }
                JSONObject payload = new JSONObject();
                payload.put("list", array);
                payload.put("total", list.size());

                this.cellet.speak(this.talkContext,
                        this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, payload));
                markResponseTime();
                return;
            }
            else if (ACTION_INSERT_ATOMS.equalsIgnoreCase(action)) {
                List<Atom> list = new ArrayList<>();
                if (requestData.has("list")) {
                    JSONArray array = requestData.getJSONArray("list");
                    for (int i = 0; i < array.length(); ++i) {
                        Atom atom = new Atom(array.getJSONObject(i));
                        if (!Atom.checkLabel(atom)) {
                            continue;
                        }

                        list.add(atom);
                    }
                }
                else {
                    JSONObject json = requestData.getJSONObject("atom");
                    Atom atom = new Atom(json);
                    if (Atom.checkLabel(atom)) {
                        list.add(atom);
                    }
                }

                int num = service.getStorage().insertAtoms(list);
                if (num >= 0) {
                    JSONObject responseData = new JSONObject();
                    responseData.put("total", num);
                    this.cellet.speak(this.talkContext,
                            this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, responseData));
                    markResponseTime();
                    return;
                }
            }
            else if (ACTION_DELETE_ATOMS.equalsIgnoreCase(action)) {
                List<Long> list = new ArrayList<>();
                JSONArray array = requestData.getJSONArray("snList");
                for (int i = 0; i < array.length(); ++i) {
                    list.add(array.getLong(i));
                }

                int num = service.getStorage().deleteAtoms(list);
                if (num >= 0) {
                    JSONObject responseData = new JSONObject();
                    responseData.put("total", num);
                    this.cellet.speak(this.talkContext,
                            this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, responseData));
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
