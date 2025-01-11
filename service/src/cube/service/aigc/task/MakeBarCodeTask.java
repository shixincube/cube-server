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
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.entity.BarCode;
import cube.common.entity.FileLabel;
import cube.common.state.AIGCStateCode;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.ToolKit;
import cube.util.PrintUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 评估回答。
 */
public class MakeBarCodeTask extends ServiceTask {

    public MakeBarCodeTask(Cellet cellet, TalkContext talkContext, Primitive primitive, ResponseTime responseTime) {
        super(cellet, talkContext, primitive, responseTime);
    }

    @Override
    public void run() {
        ActionDialect dialect = new ActionDialect(this.primitive);
        Packet packet = new Packet(dialect);

        String tokenCode = this.getTokenCode(dialect);
        if (null == tokenCode) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.NoToken.code, new JSONObject()));
            markResponseTime();
            return;
        }

        AIGCService service = ((AIGCCellet) this.cellet).getService();
        AuthToken token = service.getToken(tokenCode);
        if (null == token) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        if (!packet.data.has("list")) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(dialect, packet, AIGCStateCode.InvalidParameter.code, new JSONObject()));
            markResponseTime();
            return;
        }

        JSONArray result = new JSONArray();
        int amount = 0;

        boolean merge = packet.data.has("merge") && packet.data.getBoolean("merge");
        String paper = packet.data.has("paper") ? packet.data.getString("paper") : null;

        try {
            if (merge) {
                List<BarCode> list = new ArrayList<>();
                JSONArray array = packet.data.getJSONArray("list");
                for (int i = 0; i < array.length(); ++i) {
                    BarCode barCode = new BarCode(array.getJSONObject(i));
                    list.add(barCode);
                }
                FileLabel fileLabel = ToolKit.getInstance().makeBarCodeA4Paper(token, list);
                if (null != fileLabel) {
                    ++amount;
                    result.put(fileLabel.toJSON());
                }
            }
            else {
                JSONArray array = packet.data.getJSONArray("list");
                for (int i = 0; i < array.length(); ++i) {
                    JSONObject info = array.getJSONObject(i);
                    BarCode barCode = new BarCode(info);

                    if (null != paper) {
                        FileLabel fileLabel = ToolKit.getInstance().makeBarCodePaper(token, barCode, PrintUtils.PaperA4Ultra);
                        if (null != fileLabel) {
                            ++amount;
                            result.put(fileLabel.toJSON());
                        }
                        else {
                            result.put(new JSONObject());
                        }
                    }
                    else {
                        FileLabel fileLabel = ToolKit.getInstance().makeBarCode(token, barCode);
                        if (null != fileLabel) {
                            ++amount;
                            result.put(fileLabel.toJSON());
                        }
                        else {
                            result.put(new JSONObject());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.e(this.getClass(), "#run", e);
        }

        JSONObject responseJson = new JSONObject();
        responseJson.put("list", result);
        responseJson.put("amount", amount);

        this.cellet.speak(this.talkContext,
                this.makeResponse(dialect, packet, AIGCStateCode.Ok.code, responseJson));
        markResponseTime();
    }
}
